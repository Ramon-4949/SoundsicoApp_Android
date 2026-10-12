import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { PGlite } from '@electric-sql/pglite';

test('Android alarm migration preserves leases and restricts the queue to the worker', async () => {
  const db = new PGlite();
  const id = value => `00000000-0000-0000-0000-${String(value).padStart(12, '0')}`;
  try {
    await db.exec(`
      create role anon; create role authenticated; create role service_role;
      create schema auth;
      create function auth.uid() returns uuid language sql as $$select '${id(1)}'::uuid$$;
      create table perfiles(id uuid primary key);
      create table notificaciones_app(id uuid primary key,perfil_id uuid references perfiles(id),
        titulo text,mensaje text,payload jsonb not null default '{}');
      grant usage on schema public,auth to anon,authenticated,service_role;
      insert into perfiles values('${id(1)}');
    `);
    await db.exec(readFileSync(new URL('./android_push.sql', import.meta.url), 'utf8'));
    const migration = readFileSync(new URL('./android_alarm_notifications.sql', import.meta.url), 'utf8');
    await db.exec(migration);
    await db.exec(migration);
    await db.query('select register_android_push_device($1,$2)', [id(2), 'test-device-token-long-enough']);
    await db.query('insert into notificaciones_app values($1,$2,$3,$4,$5)',
      [id(3),id(1),'Hito vencido','Confirma el hito', { is_alarm: true }]);
    await db.query('insert into notificaciones_app values($1,$2,$3,$4,$5)',
      [id(4),id(1),'Comunicado','Mensaje general', {}]);
    await db.exec('set role authenticated');
    await assert.rejects(db.query('select * from claim_android_pushes_v2()'), /permission denied/);
    await db.exec('set role service_role');
    const jobs = (await db.query('select * from claim_android_pushes_v2()')).rows;
    assert.equal(jobs.length, 2);
    assert.equal(jobs.find(job => job.notification_id === id(3)).is_alarm, true);
    assert.equal(jobs.find(job => job.notification_id === id(4)).is_alarm, false);
    assert.equal((await db.query('select * from claim_android_pushes_v2()')).rows.length, 0);
    for (const job of jobs) {
      await db.query('select finish_android_push($1,$2,true,null,false)', [job.job_id, job.lease]);
    }
    await db.exec('reset role');
    assert.equal((await db.query('select count(*)::int count from android_push_private.outbox where sent_at is not null')).rows[0].count, 2);
  } finally {
    await db.close();
  }
});
