import test from 'node:test';
import assert from 'node:assert/strict';
import { messageFor, runBatch, configuration } from './worker.mjs';

const job = { job_id: 'job', lease: 'lease', notification_id: 'notification', recipient: 'recipient', token: 'token', title: 'Montaje', body: 'Confirmación' };

test('payload uses validated recipient routing and data-only delivery', () => {
  const message = messageFor(job);
  assert.equal(message.data.notification_id, job.notification_id);
  assert.equal(message.data.recipient_id, job.recipient);
  assert.equal(message.notification, undefined);
  assert.equal(message.android.priority, 'high');
});

test('successful send acknowledges the exact lease', async () => {
  const calls = [];
  await runBatch({}, async () => 'sent', async (_, name, params) => {
    calls.push([name, params]);
    return name.startsWith('claim_') ? [job] : null;
  });
  assert.deepEqual(calls[1][1], { p_job: 'job', p_lease: 'lease', p_success: true, p_error: null, p_invalid: false });
});

test('only an unregistered token invalidates a device', async () => {
  for (const code of ['messaging/registration-token-not-registered', 'messaging/mismatched-credential', 'messaging/internal-error']) {
    let finish;
    await runBatch({}, async () => { throw { code }; }, async (_, name, params) => {
      if (name.startsWith('claim_')) return [job];
      finish = params;
    });
    assert.equal(finish.p_invalid, code === 'messaging/registration-token-not-registered');
    assert.equal(finish.p_success, false);
  }
});

test('acknowledgment failures are reported so leases can retry', async () => {
  await assert.rejects(runBatch({}, async () => 'sent', async (_, name) => {
    if (name.startsWith('claim_')) return [job];
    throw new Error('unavailable');
  }));
});

test('server requires credentials and https', () => {
  assert.throws(() => configuration({}));
  assert.throws(() => configuration({ SUPABASE_URL: 'http://example.com', SUPABASE_SERVICE_ROLE_KEY: 'key', GOOGLE_APPLICATION_CREDENTIALS: 'file' }));
});
