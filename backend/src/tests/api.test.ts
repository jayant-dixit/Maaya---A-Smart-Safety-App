import { generateEmergencyAccessToken, verifyEmergencyAccessToken } from '../services/tokenService';
import { SmsService } from '../services/smsService';

async function runTests() {
  console.log('--- RUNNING BACKEND UNIT TESTS ---');

  // Test 1: JWT Token Generation & Verification
  const testIncidentId = 'test-incident-uuid-1234';
  const token = generateEmergencyAccessToken(testIncidentId, 3600);
  console.log('Test 1: Generated Token:', token.slice(0, 25) + '...');
  
  const decoded = verifyEmergencyAccessToken(token);
  if (decoded && decoded.incidentId === testIncidentId && decoded.role === 'EMERGENCY_VIEWER') {
    console.log('✅ Test 1 PASSED: Token verified and claims match.');
  } else {
    console.error('❌ Test 1 FAILED: Token verification mismatch.');
    process.exit(1);
  }

  // Test 2: Expired Token Rejection
  const expiredToken = generateEmergencyAccessToken(testIncidentId, -10);
  const expiredDecoded = verifyEmergencyAccessToken(expiredToken);
  if (expiredDecoded === null) {
    console.log('✅ Test 2 PASSED: Expired token rejected as expected.');
  } else {
    console.error('❌ Test 2 FAILED: Expired token was accepted.');
    process.exit(1);
  }

  // Test 3: SMS Service Mock Dispatch
  const sms = new SmsService();
  const smsResult = await sms.dispatchEmergencyAlert({
    to: '+919876543210',
    recipientName: 'Mom',
    senderName: 'Priya',
    latitude: 28.6139,
    longitude: 77.2090,
    viewerUrl: 'http://localhost:5173/emergency/test-incident',
  });

  if (smsResult.success) {
    console.log('✅ Test 3 PASSED: SMS dispatched successfully via adapter.');
  } else {
    console.error('❌ Test 3 FAILED: SMS dispatch returned error.');
    process.exit(1);
  }

  console.log('--- ALL BACKEND TESTS PASSED (3/3) ---');
}

runTests().catch((err) => {
  console.error(err);
  process.exit(1);
});
