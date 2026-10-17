import { Pool } from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const connectionString = process.env.DATABASE_URL || 'postgresql://maaya_admin:maaya_secure_dev_password@localhost:5432/maaya_db';

export const pool = new Pool({
  connectionString,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 3000,
});

let isPostgresAvailable = false;

export async function testDbConnection(): Promise<boolean> {
  try {
    const client = await pool.connect();
    client.release();
    isPostgresAvailable = true;
    console.log('[Database] PostgreSQL connected successfully.');
    return true;
  } catch (err: any) {
    console.warn('[Database] PostgreSQL connection failed (falling back to resilient memory store):', err.message);
    isPostgresAvailable = false;
    return false;
  }
}

export function isDbConnected(): boolean {
  return isPostgresAvailable;
}
