export type WebSocketHandler = (event: { type: string; data: any }) => void;

export class EmergencyWsClient {
  private ws: WebSocket | null = null;
  private url: string;
  private onMessageCallback: WebSocketHandler;
  private shouldReconnect = true;
  private reconnectAttempts = 0;

  constructor(incidentId: string, token: string, onMessage: WebSocketHandler) {
    const host = (typeof window !== 'undefined' && window.location.hostname) ? window.location.hostname : 'localhost';
    this.url = `ws://${host}:8088/ws/emergency?incidentId=${encodeURIComponent(incidentId)}&token=${encodeURIComponent(token)}`;
    this.onMessageCallback = onMessage;
    this.connect();
  }

  private connect() {
    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('[WebSocket Client] Connected to emergency live channel.');
        this.reconnectAttempts = 0;
      };

      this.ws.onmessage = (event) => {
        try {
          const parsed = JSON.parse(event.data);
          this.onMessageCallback(parsed);
        } catch (e) {
          console.error('[WebSocket Client] Malformed JSON:', event.data);
        }
      };

      this.ws.onclose = () => {
        console.warn('[WebSocket Client] Connection closed.');
        if (this.shouldReconnect) {
          const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 10000);
          this.reconnectAttempts++;
          setTimeout(() => this.connect(), delay);
        }
      };

      this.ws.onerror = (err) => {
        console.error('[WebSocket Client] Error:', err);
      };
    } catch (e) {
      console.error('[WebSocket Client] Connection creation error:', e);
    }
  }

  disconnect() {
    this.shouldReconnect = false;
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}
