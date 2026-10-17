import { Router, Request, Response } from 'express';
import crypto from 'crypto';
import path from 'path';
import fs from 'fs';
import { generateEmergencyAccessToken, verifyEmergencyAccessToken } from '../services/tokenService';
import { pool, isDbConnected } from '../config/db';
import { EmergencyWsServer } from '../websocket/wsServer';

// Resilient in-memory store for dev or when postgres container is booting
export const inMemoryIncidents = new Map<string, any>();
export const inMemoryLocations = new Map<string, any[]>();

export function createIncidentRouter(wsServer: EmergencyWsServer) {
  const router = Router();
  const dashboardBaseUrl = process.env.DASHBOARD_URL || 'http://192.168.29.112:5173';

  // 1. Create emergency incident
  router.post('/', async (req: Request, res: Response): Promise<any> => {
    try {
      const { id, triggerType, latitude, longitude, accuracy, heartRateBpm, batteryLevel, notes } = req.body;
      const incidentId = id || req.body.incidentId || crypto.randomUUID();
      const accessToken = generateEmergencyAccessToken(incidentId, 3600); // 1-hour expiration
      const viewerUrl = `${dashboardBaseUrl}/emergency/${incidentId}?token=${accessToken}&incidentId=${incidentId}`;

      const incident = {
        incidentId,
        triggerType: triggerType || 'MANUAL_IN_APP',
        status: 'ACTIVE',
        initialLatitude: latitude || 0.0,
        initialLongitude: longitude || 0.0,
        currentLatitude: latitude || 0.0,
        currentLongitude: longitude || 0.0,
        accuracyMeters: accuracy || 10,
        heartRateBpm: heartRateBpm || null,
        batteryPercentage: batteryLevel || null,
        dashboardAccessToken: accessToken,
        dashboardViewerUrl: viewerUrl,
        streamToken: accessToken,
        notes: notes || null,
        createdAt: Date.now(),
      };

      // Persist to memory store
      inMemoryIncidents.set(incidentId, incident);
      inMemoryLocations.set(incidentId, [{
        latitude: incident.currentLatitude,
        longitude: incident.currentLongitude,
        accuracy: incident.accuracyMeters,
        heartRateBpm: incident.heartRateBpm,
        batteryPercentage: incident.batteryPercentage,
        timestamp: Date.now(),
      }]);

      // If PostgreSQL available, also insert into DB
      if (isDbConnected()) {
        try {
          await pool.query(
            `INSERT INTO emergency_incidents 
            (id, trigger_type, status, initial_latitude, initial_longitude, current_latitude, current_longitude, accuracy_meters, heart_rate_bpm, battery_percentage, dashboard_access_token, notes) 
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
            [incidentId, incident.triggerType, incident.status, incident.initialLatitude, incident.initialLongitude, incident.currentLatitude, incident.currentLongitude, incident.accuracyMeters, incident.heartRateBpm, incident.batteryPercentage, accessToken, notes]
          );
        } catch (dbErr) {
          console.warn('[Database] Failed to write incident to PG, kept in memory:', dbErr);
        }
      }

      console.log(`[Incident] Created SOS: ${incidentId} (Trigger: ${incident.triggerType})`);
      return res.status(201).json(incident);
    } catch (err: any) {
      console.error('[Incident] Error creating incident:', err);
      return res.status(500).json({ error: 'Failed to create emergency incident' });
    }
  });

  // 2. Real-time Location & Telemetry update
  router.post('/location', async (req: Request, res: Response): Promise<any> => {
    try {
      const { incidentId, latitude, longitude, accuracy, speed, altitude, heartRateBpm, batteryPercentage } = req.body;

      if (!incidentId || latitude == null || longitude == null) {
        return res.status(400).json({ error: 'incidentId, latitude, and longitude are required' });
      }

      let existing = inMemoryIncidents.get(incidentId);
      if (existing) {
        existing.currentLatitude = latitude;
        existing.currentLongitude = longitude;
        existing.accuracyMeters = accuracy;
        if (heartRateBpm) existing.heartRateBpm = heartRateBpm;
        if (batteryPercentage) existing.batteryPercentage = batteryPercentage;
      } else {
        const accessToken = generateEmergencyAccessToken(incidentId, 3600);
        const viewerUrl = `${dashboardBaseUrl}/emergency/${incidentId}?token=${accessToken}&incidentId=${incidentId}`;
        existing = {
          incidentId,
          triggerType: 'MANUAL_IN_APP',
          status: 'ACTIVE',
          initialLatitude: latitude,
          initialLongitude: longitude,
          currentLatitude: latitude,
          currentLongitude: longitude,
          accuracyMeters: accuracy || 10,
          heartRateBpm: heartRateBpm || null,
          batteryPercentage: batteryPercentage || null,
          dashboardAccessToken: accessToken,
          dashboardViewerUrl: viewerUrl,
          streamToken: accessToken,
          notes: 'Auto-synchronized telemetry session',
          createdAt: Date.now(),
        };
        inMemoryIncidents.set(incidentId, existing);
      }

      const locPoint = {
        incidentId,
        latitude,
        longitude,
        accuracy: accuracy || 5,
        speed: speed || null,
        altitude: altitude || null,
        heartRateBpm: heartRateBpm || null,
        batteryPercentage: batteryPercentage || null,
        timestamp: Date.now(),
      };

      const locs = inMemoryLocations.get(incidentId) || [];
      locs.push(locPoint);
      inMemoryLocations.set(incidentId, locs);

      // Broadcast over WebSockets to authorized viewers in real-time
      wsServer.broadcastToIncident(incidentId, 'LOCATION_UPDATE', locPoint);

      if (isDbConnected()) {
        try {
          await pool.query(
            `UPDATE emergency_incidents SET current_latitude = $1, current_longitude = $2, accuracy_meters = $3, heart_rate_bpm = COALESCE($4, heart_rate_bpm), battery_percentage = COALESCE($5, battery_percentage) WHERE id = $6`,
            [latitude, longitude, accuracy, heartRateBpm, batteryPercentage, incidentId]
          );
          await pool.query(
            `INSERT INTO incident_locations (incident_id, latitude, longitude, accuracy_meters, speed, altitude, heart_rate_bpm, battery_percentage) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
            [incidentId, latitude, longitude, accuracy, speed, altitude, heartRateBpm, batteryPercentage]
          );
        } catch (dbErr) {
          // resilient
        }
      }

      return res.json({ success: true, message: 'Telemetry updated and broadcasted' });
    } catch (err: any) {
      return res.status(500).json({ error: 'Failed to update location' });
    }
  });

  // 3. Resolve Emergency Incident
  router.post('/resolve', async (req: Request, res: Response): Promise<any> => {
    try {
      const { incidentId, id, resolutionReason } = req.body;
      const targetId = incidentId || id;

      if (!targetId) {
        // If no ID specified, resolve the latest active incident
        const activeIncidents = Array.from(inMemoryIncidents.values()).filter((i) => i.status === 'ACTIVE');
        if (activeIncidents.length > 0) {
          activeIncidents.forEach((inc) => {
            inc.status = 'RESOLVED';
            inc.resolvedAt = Date.now();
            inc.resolutionReason = resolutionReason || 'Emergency Resolved & Marked Safe';
            wsServer.broadcastToIncident(inc.incidentId, 'INCIDENT_RESOLVED', {
              incidentId: inc.incidentId,
              resolvedAt: Date.now(),
              reason: inc.resolutionReason,
            });
          });
          return res.json({ success: true, message: 'All active incidents resolved' });
        }
      }

      let existing = inMemoryIncidents.get(targetId);
      if (existing) {
        existing.status = 'RESOLVED';
        existing.resolvedAt = Date.now();
        existing.resolutionReason = resolutionReason || 'Emergency Resolved & Marked Safe';
      } else if (targetId) {
        existing = {
          incidentId: targetId,
          triggerType: 'MANUAL_IN_APP',
          status: 'RESOLVED',
          initialLatitude: 0,
          initialLongitude: 0,
          currentLatitude: 0,
          currentLongitude: 0,
          accuracyMeters: 0,
          resolvedAt: Date.now(),
          resolutionReason: resolutionReason || 'Emergency Resolved & Marked Safe',
          createdAt: Date.now() - 60000,
        };
        inMemoryIncidents.set(targetId, existing);
      }

      console.log(`[Incident] Emergency SOS RESOLVED for incident ${targetId}`);

      wsServer.broadcastToIncident(targetId, 'INCIDENT_RESOLVED', {
        incidentId: targetId,
        resolvedAt: Date.now(),
        reason: resolutionReason || 'Emergency Resolved & Marked Safe',
      });

      if (isDbConnected() && targetId) {
        try {
          await pool.query(
            `UPDATE emergency_incidents SET status = 'RESOLVED', resolved_at = CURRENT_TIMESTAMP WHERE id = $1`,
            [targetId]
          );
        } catch (_) {}
      }

      return res.json({ success: true, message: 'Incident resolved successfully', incident: existing });
    } catch (err) {
      return res.status(500).json({ error: 'Failed to resolve incident' });
    }
  });

  // 4. Live Video Frame Broadcast (from Android camera)
  router.post('/:id/stream-frame', (req: Request, res: Response): any => {
    const { id } = req.params;
    const { frame, audioLevel } = req.body;

    if (!frame && audioLevel == null) {
      return res.status(400).json({ error: 'Missing frame or audioLevel data' });
    }

    let existing = inMemoryIncidents.get(id);
    if (!existing) {
      const active = Array.from(inMemoryIncidents.values()).reverse().find(i => i.status === 'ACTIVE');
      if (active) existing = active;
    }

    if (existing) {
      if (frame) existing.lastLiveFrame = frame;
      if (audioLevel != null) existing.lastAudioLevel = audioLevel;
    }

    const payload = {
      frame: frame || existing?.lastLiveFrame || null,
      audioLevel: audioLevel ?? existing?.lastAudioLevel ?? 0,
      timestamp: Date.now(),
    };

    wsServer.broadcastToIncident(id, 'LIVE_FRAME', payload);

    return res.json({ success: true });
  });

  // 5. Emergency Audio/Video Recording Upload
  router.post('/:id/recording', (req: Request, res: Response): any => {
    try {
      const { id } = req.params;
      const recordingsDir = path.join(__dirname, '../../public/recordings');
      if (!fs.existsSync(recordingsDir)) {
        fs.mkdirSync(recordingsDir, { recursive: true });
      }

      const fileName = `recording_${id}_${Date.now()}.mp4`;
      const filePath = path.join(recordingsDir, fileName);

      if (Buffer.isBuffer(req.body)) {
        fs.writeFileSync(filePath, req.body);
      } else if (req.body && req.body.audioBase64) {
        const base64Data = req.body.audioBase64.replace(/^data:([A-Za-z-+/]+);base64,/, '');
        fs.writeFileSync(filePath, Buffer.from(base64Data, 'base64'));
      } else if (typeof req.body === 'string') {
        fs.writeFileSync(filePath, Buffer.from(req.body, 'base64'));
      }

      const recordingUrl = `/recordings/${fileName}`;
      const existing = inMemoryIncidents.get(id);
      if (existing) {
        existing.recordingUrl = recordingUrl;
      }

      console.log(`[Media] Saved emergency recording for incident ${id}: ${recordingUrl}`);

      wsServer.broadcastToIncident(id, 'RECORDING_READY', {
        incidentId: id,
        recordingUrl,
        timestamp: Date.now(),
      });

      return res.json({ success: true, recordingUrl });
    } catch (err: any) {
      console.error('[Media] Recording save error:', err);
      return res.status(500).json({ error: 'Failed to save recording' });
    }
  });

  // 6. Get Incident by ID
  router.get('/:id', async (req: Request, res: Response): Promise<any> => {
    const { id } = req.params;
    let incident = inMemoryIncidents.get(id);

    // If ID is not found (e.g. demo link or latest SOS), return the most recent active incident
    if (!incident) {
      const activeIncidents = Array.from(inMemoryIncidents.values()).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
      incident = activeIncidents.find((i) => i.status === 'ACTIVE') || activeIncidents[0];
    }

    if (!incident) {
      return res.status(404).json({ error: 'No emergency incidents found' });
    }

    const locations = inMemoryLocations.get(incident.incidentId) || [];
    return res.json({
      ...incident,
      locationHistory: locations,
    });
  });

  // 7. List all active incidents (sorted newest first)
  router.get('/', (_req: Request, res: Response) => {
    const list = Array.from(inMemoryIncidents.values())
      .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
      .map((inc) => ({
        ...inc,
        locationHistory: inMemoryLocations.get(inc.incidentId) || [],
      }));
    res.json(list);
  });

  // 8. Delete / Prune an incident
  router.delete('/:id', (req: Request, res: Response): any => {
    const { id } = req.params;
    inMemoryIncidents.delete(id);
    inMemoryLocations.delete(id);
    return res.json({ success: true, message: `Incident ${id} deleted` });
  });

  return router;
}
