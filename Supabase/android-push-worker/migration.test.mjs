import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { PGlite } from '@electric-sql/pglite';

test('migration isolates recipients, leases jobs, handles rotation and never replays history', async () => {
  const db = new PGlite();
  try {
    await db.exec(`
      create role anon;
      create role authenticated;
      create role service_role;
      create schema auth;
      create function auth.uid() returns uuid language sql as
        'select nullif(current_setting(''request.jwt.claim.sub'', true), '''')::uuid';
      create function auth.role() returns text language sql as 'select ''authenticated''::text';
      create function public.account_is_approved() returns boolean language sql as 'select false';
      create table public.perfiles(id uuid primary key);
      create table public.notificaciones_app(id uuid primary key, perfil_id uuid, titulo text, mensaje text);
      insert into public.perfiles values ('00000000-0000-0000-0000-000000000001'), ('00000000-0000-0000-0000-000000000002');
      insert into public.notificaciones_app values ('00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000001','Antigua','Historial');
    `);
    const migration = await readFile(new URL('../android_push.sql', import.meta.url), 'utf8');
    await db.exec(migration);
    await db.exec(migration);
    await db.query("select set_config('request.path', 'rpc/register_android_push_device', false)");
    await db.query('select public.check_account_access()');
    await db.query("select set_config('request.path', 'asignaciones', false)");
    await assert.rejects(db.query('select public.check_account_access()'));

    await db.query("select set_config('request.jwt.claim.sub', $1, false)", ['00000000-0000-0000-0000-000000000001']);
    await db.query('select public.register_android_push_device($1,$2)', ['00000000-0000-0000-0000-000000000004', 'token-android-original-1234567890']);
    assert.equal((await db.query('select * from public.claim_android_notification_pushes()')).rows.length, 0);
    await db.exec(`
      insert into public.notificaciones_app values ('00000000-0000-0000-0000-000000000005','00000000-0000-0000-0000-000000000001','Nueva','Montaje');
      insert into public.notificaciones_app values ('00000000-0000-0000-0000-000000000006','00000000-0000-0000-0000-000000000002','Otro','Privado');
    `);
    const jobs = (await db.query('select * from public.claim_android_notification_pushes()')).rows;
    assert.equal(jobs.length, 1);
    assert.equal(jobs[0].title, 'Nueva');
    assert.equal((await db.query('select * from public.claim_android_notification_pushes()')).rows.length, 0);
    await db.query('select public.finish_android_notification_push($1,$2,true,null,false)', [jobs[0].job_id, '00000000-0000-0000-0000-000000000007']);
    assert.equal((await db.query('select sent_at from android_push_private.outbox')).rows[0].sent_at, null);
    await db.query('select public.register_android_push_device($1,$2)', ['00000000-0000-0000-0000-000000000004', 'token-android-rotated-1234567890']);
    await db.query('select public.finish_android_notification_push($1,$2,false,$3,true)', [jobs[0].job_id, jobs[0].lease, 'messaging/registration-token-not-registered']);
    assert.equal((await db.query('select token from android_push_private.devices')).rows[0].token, 'token-android-rotated-1234567890');
    await db.query("select set_config('request.jwt.claim.sub', $1, false)", ['00000000-0000-0000-0000-000000000002']);
    await db.query('select public.register_android_push_device($1,$2)', ['00000000-0000-0000-0000-000000000004', 'token-android-rotated-1234567890']);
    assert.equal((await db.query('select * from android_push_private.outbox')).rows.length, 0);
    await db.query('select public.unregister_android_push_device($1)', ['00000000-0000-0000-0000-000000000004']);
    assert.equal((await db.query('select * from android_push_private.devices')).rows.length, 0);
    const privileges = await db.query("select has_function_privilege('authenticated','public.claim_android_notification_pushes()','execute') as allowed");
    assert.equal(privileges.rows[0].allowed, false);
  } finally { await db.close(); }
});
