import { createServer } from 'node:http';
import { setTimeout as delay } from 'node:timers/promises';
import { pathToFileURL } from 'node:url';
import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

export function messageFor(job) {
  const text = (value, fallback, maximum) => Array.from(value?.trim() || fallback).slice(0, maximum).join('');
  return {
    token: job.token,
    data: {
      notification_id: job.notification_id,
      recipient_id: job.recipient,
      title: text(job.title, 'SounDisco', 120),
      body: text(job.body, 'Tienes una actualización. Abre la app para ver los detalles.', 500)
    },
    android: { priority: 'high', ttl: 3600000 }
  };
}

export async function rpc(config, name, params = {}) {
  const headers = { apikey: config.key, 'Content-Type': 'application/json' };
  if (!config.key.startsWith('sb_secret_')) headers.Authorization = 'Bearer ' + config.key;
  const response = await fetch(config.url + '/rest/v1/rpc/' + name, {
    method: 'POST', headers, body: JSON.stringify(params), redirect: 'error', signal: AbortSignal.timeout(20000)
  });
  if (!response.ok) throw new Error('Supabase RPC failed: ' + response.status);
  const body = await response.text();
  return body ? JSON.parse(body) : null;
}

export async function runBatch(config, send, call = rpc) {
  const jobs = await call(config, 'claim_android_notification_pushes');
  let failedAcknowledgments = 0;
  for (let offset = 0; offset < jobs.length; offset += 5) {
    const results = await Promise.allSettled(jobs.slice(offset, offset + 5).map(async job => {
      let success = false;
      let invalid = false;
      let error = null;
      try {
        await send(messageFor(job));
        success = true;
      } catch (failure) {
        invalid = failure.code === 'messaging/registration-token-not-registered';
        error = typeof failure.code === 'string' && /^messaging\/[a-z-]+$/.test(failure.code)
          ? failure.code : 'transport-error';
      }
      await call(config, 'finish_android_notification_push', {
        p_job: job.job_id, p_lease: job.lease, p_success: success, p_error: error, p_invalid: invalid
      });
    }));
    failedAcknowledgments += results.filter(result => result.status === 'rejected').length;
  }
  if (failedAcknowledgments) throw new Error('Push acknowledgments failed');
  return jobs.length;
}

export function configuration(env = process.env) {
  if (!env.SUPABASE_URL || !env.SUPABASE_SERVICE_ROLE_KEY || !env.GOOGLE_APPLICATION_CREDENTIALS)
    throw new Error('Missing server configuration');
  const url = new URL(env.SUPABASE_URL);
  if (url.protocol !== 'https:' || url.pathname !== '/' || url.search || url.hash || url.username || url.password)
    throw new Error('SUPABASE_URL must be an HTTPS origin');
  const port = Number(env.PORT || 10000);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PORT');
  return { url: url.origin, key: env.SUPABASE_SERVICE_ROLE_KEY, port };
}

async function main() {
  const config = configuration();
  initializeApp({ credential: applicationDefault() });
  const messaging = getMessaging();
  let lastSuccess = 0;
  const server = createServer((request, response) => {
    if (request.url !== '/health' && request.url !== '/') {
      response.writeHead(404).end();
      return;
    }
    response.writeHead(Date.now() - lastSuccess < 300000 ? 200 : 503, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({ service: 'soundisco-android-push', healthy: Date.now() - lastSuccess < 300000 }));
  }).listen(config.port, '0.0.0.0');
  const shutdown = new AbortController();
  for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => shutdown.abort());
  try {
    while (!shutdown.signal.aborted) {
      try {
        const processed = await runBatch(config, message => messaging.send(message));
        lastSuccess = Date.now();
        console.info(JSON.stringify({ processed }));
      } catch {
        console.error('Android push cycle failed. Check configuration and private outbox.');
      }
      try { await delay(10000, undefined, { signal: shutdown.signal }); } catch { break; }
    }
  } finally { server.close(); }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(() => { console.error('Android push startup failed. Check server configuration.'); process.exitCode = 1; });
}
