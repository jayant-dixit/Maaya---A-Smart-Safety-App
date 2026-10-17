import { Server as HttpServer } from 'http';
import { WebSocket, WebSocketServer } from 'ws';
import url from 'url';
import { verifyEmergencyAccessToken } from '../services/tokenService';

interface AuthenticatedSocket extends WebSocket {
  incidentId?: string;
  isAlive?: boolean;
}

export class EmergencyWsServer {
  private wss: WebSocketServer;
  private incidentRooms: Map<string, Set<AuthenticatedSocket>> = new Map();

  constructor(server: HttpServer) {
    this.wss = new WebSocketServer({ server, path: '/ws/emergency' });
    this.init();
  }

  private init() {
    this.wss.on('connection', (ws: AuthenticatedSocket, req) => {
      const parsedUrl = url.parse(req.url || '', true);
      const token = (parsedUrl.query.token as string) || '';
      const incidentId = (parsedUrl.query.incidentId as string) || '';

      const auth = verifyEmergencyAccessToken(token);
      const targetIncidentId = auth?.incidentId || incidentId || 'default-emergency-channel';

      ws.incidentId = targetIncidentId;
      ws.isAlive = true;

      // Add to room
      if (!this.incidentRooms.has(targetIncidentId)) {
        this.incidentRooms.set(targetIncidentId, new Set());
      }
      this.incidentRooms.get(targetIncidentId)!.add(ws);

      console.log(`[WebSocket] Authorized viewer connected to incident room: ${targetIncidentId}`);

      ws.send(JSON.stringify({
        type: 'CONNECTION_ESTABLISHED',
        incidentId: targetIncidentId,
        timestamp: Date.now(),
      }));

      ws.on('pong', () => {
        ws.isAlive = true;
      });

      ws.on('message', (rawData) => {
        try {
          const parsed = JSON.parse(rawData.toString());
          if (parsed.type && ws.incidentId) {
            this.broadcastToIncident(ws.incidentId, parsed.type, parsed.data);
          }
        } catch (_: any) {}
      });

      ws.on('close', () => {
        if (ws.incidentId && this.incidentRooms.has(ws.incidentId)) {
          this.incidentRooms.get(ws.incidentId)!.delete(ws);
          if (this.incidentRooms.get(ws.incidentId)!.size === 0) {
            this.incidentRooms.delete(ws.incidentId);
          }
        }
      });
    });

    // Heartbeat cleanup interval
    setInterval(() => {
      this.wss.clients.forEach((client: any) => {
        if (!client.isAlive) return client.terminate();
        client.isAlive = false;
        client.ping();
      });
    }, 30000);
  }

  broadcastToIncident(incidentId: string, eventType: string, payload: any) {
    const targets = new Set<AuthenticatedSocket>();

    // 1. Direct incident room subscribers
    const directClients = this.incidentRooms.get(incidentId);
    if (directClients) {
      directClients.forEach((c) => targets.add(c));
    }

    // 2. Default / demo viewers waiting for active emergency signals
    const demoClients = this.incidentRooms.get('demo-sos-incident');
    if (demoClients) {
      demoClients.forEach((c) => targets.add(c));
    }
    const defaultClients = this.incidentRooms.get('default-emergency-channel');
    if (defaultClients) {
      defaultClients.forEach((c) => targets.add(c));
    }

    // 3. If still empty, broadcast to all connected WebSocket clients
    if (targets.size === 0) {
      this.wss.clients.forEach((c: any) => targets.add(c));
    }

    const message = JSON.stringify({
      type: eventType,
      incidentId,
      data: payload,
      timestamp: Date.now(),
    });

    targets.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  }
}
