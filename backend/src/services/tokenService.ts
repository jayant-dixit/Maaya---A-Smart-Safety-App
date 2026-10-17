import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'dev_super_secret_jwt_key_change_in_production_32chars';

export interface EmergencyViewerPayload {
  incidentId: string;
  role: 'EMERGENCY_VIEWER';
  scope: 'READ_TELEMETRY_AND_STREAM';
}

export function generateEmergencyAccessToken(incidentId: string, expiresInSeconds: number = 3600): string {
  const payload: EmergencyViewerPayload = {
    incidentId,
    role: 'EMERGENCY_VIEWER',
    scope: 'READ_TELEMETRY_AND_STREAM',
  };

  return jwt.sign(payload, JWT_SECRET, { expiresIn: expiresInSeconds });
}

export function verifyEmergencyAccessToken(token: string): EmergencyViewerPayload | null {
  try {
    const decoded = jwt.verify(token, JWT_SECRET) as EmergencyViewerPayload;
    if (decoded.role === 'EMERGENCY_VIEWER') {
      return decoded;
    }
    return null;
  } catch (err) {
    return null;
  }
}
