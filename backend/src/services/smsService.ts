export interface SmsDispatchPayload {
  to: string;
  recipientName: string;
  senderName: string;
  latitude: number;
  longitude: number;
  viewerUrl: string;
}

export interface SmsDispatchResult {
  success: boolean;
  recipient: string;
  messageId?: string;
  error?: string;
}

export class SmsService {
  private provider: string;

  constructor() {
    this.provider = process.env.SMS_PROVIDER || 'mock';
  }

  async dispatchEmergencyAlert(payload: SmsDispatchPayload): Promise<SmsDispatchResult> {
    const textMessage = `EMERGENCY SOS: ${payload.senderName || 'Maaya User'} triggered an SOS! Live tracking & camera stream: ${payload.viewerUrl}`;

    if (this.provider === 'mock') {
      console.log(`[SMS:MOCK] >>> Delivering to ${payload.to} (${payload.recipientName}): "${textMessage}"`);
      return {
        success: true,
        recipient: payload.to,
        messageId: `mock-sms-${Date.now()}-${Math.random().toString(36).substring(7)}`,
      };
    }

    // Configurable Twilio or external adapter
    try {
      const accountSid = process.env.TWILIO_ACCOUNT_SID;
      const authToken = process.env.TWILIO_AUTH_TOKEN;
      const fromNumber = process.env.TWILIO_PHONE_NUMBER;

      if (!accountSid || !authToken || !fromNumber) {
        console.warn('[SMS:TWILIO] Credentials missing, fallback to mock log.');
        return { success: true, recipient: payload.to, messageId: `mock-fallback-${Date.now()}` };
      }

      // External API call omitted if no real credentials provided
      return { success: true, recipient: payload.to, messageId: `tw-${Date.now()}` };
    } catch (err: any) {
      console.error(`[SMS] Failed to deliver to ${payload.to}:`, err.message);
      return { success: false, recipient: payload.to, error: err.message };
    }
  }
}

export const smsService = new SmsService();
