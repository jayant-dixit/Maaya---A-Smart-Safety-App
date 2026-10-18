import express from 'express';
import http from 'http';
import cors from 'cors';
import dotenv from 'dotenv';
import { testDbConnection } from './config/db';
import { EmergencyWsServer } from './websocket/wsServer';
import { createIncidentRouter } from './routes/incidentRoutes';
import { createSafeRideRouter } from './routes/safeRideRoutes';

dotenv.config();

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 8088;

import path from 'path';
import fs from 'fs';

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));
app.use(express.raw({ type: ['audio/*', 'video/*', 'application/octet-stream'], limit: '50mb' }));

const recordingsDir = path.join(__dirname, '../public/recordings');
if (!fs.existsSync(recordingsDir)) {
  fs.mkdirSync(recordingsDir, { recursive: true });
}
app.use('/recordings', express.static(recordingsDir));

// Initialize WebSocket signaling server
const wsServer = new EmergencyWsServer(server);

// API Routes
app.use('/api/incidents', createIncidentRouter(wsServer));
app.use('/api/saferide', createSafeRideRouter());

// Seamless Redirect: If emergency SMS link points to backend, redirect to Vercel dashboard
app.get('/emergency/:incidentId', (req, res) => {
  const dashboardUrl = (process.env.DASHBOARD_URL || 'https://maaya-henna.vercel.app').replace(/\/+$/, '');
  const query = req.url.includes('?') ? req.url.substring(req.url.indexOf('?')) : '';
  res.redirect(302, `${dashboardUrl}/emergency/${req.params.incidentId}${query}`);
});

app.get('/api/health', (_req, res) => {
  res.json({
    status: 'HEALTHY',
    service: 'Maaya Emergency Response Backend',
    timestamp: new Date().toISOString(),
    version: '1.0.0',
  });
});

server.listen(PORT, async () => {
  console.log(`====================================================`);
  console.log(`🛡️  MAAYA EMERGENCY BACKEND RUNNING ON PORT ${PORT}`);
  console.log(`📡 WebSocket Signaling: ws://localhost:${PORT}/ws/emergency`);
  console.log(`====================================================`);
  await testDbConnection();
});

export { app, server };
