import { Router, Request, Response } from 'express';

export const inMemorySafeRides = new Map<string, any>();

export function createSafeRideRouter() {
  const router = Router();

  router.post('/start', (req: Request, res: Response): any => {
    const { rideId, vehiclePlate, driverName, destinationName, expectedArrivalMinutes, checkInIntervalMinutes } = req.body;
    const session = {
      rideId,
      vehiclePlate,
      driverName,
      destinationName,
      expectedArrivalMinutes,
      checkInIntervalMinutes,
      startedAt: Date.now(),
      lastCheckInAt: Date.now(),
      isCompleted: false,
      isEscalatedToSos: false,
    };
    inMemorySafeRides.set(rideId, session);
    console.log(`[SafeRide] Journey started for vehicle: ${vehiclePlate}`);
    return res.status(201).json({ success: true, session });
  });

  router.post('/checkin', (req: Request, res: Response): any => {
    const { rideId, latitude, longitude } = req.body;
    const existing = inMemorySafeRides.get(rideId);
    if (existing) {
      existing.lastCheckInAt = Date.now();
      existing.lastLatitude = latitude;
      existing.lastLongitude = longitude;
    }
    return res.json({ success: true, message: 'Check-in recorded' });
  });

  router.post('/end', (req: Request, res: Response): any => {
    const { rideId } = req.body;
    const existing = inMemorySafeRides.get(rideId);
    if (existing) {
      existing.isCompleted = true;
      existing.completedAt = Date.now();
    }
    return res.json({ success: true, message: 'Journey completed safely' });
  });

  router.get('/active', (_req: Request, res: Response) => {
    const active = Array.from(inMemorySafeRides.values()).find((r) => !r.isCompleted);
    return res.json(active || null);
  });

  return router;
}
