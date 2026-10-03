begin;
create schema if not exists android_push_private;
revoke all on schema android_push_private from public, anon, authenticated;

create table if not exists android_push_private.devices (
    installation uuid primary key,
    perfil_id uuid not null references public.perfiles(id) on delete cascade,
    token text not null unique,
    updated_at timestamptz not null default now()
);
create table if not exists android_push_private.outbox (
    id uuid primary key default gen_random_uuid(),
    notification_id uuid not null references public.notificaciones_app(id) on delete cascade,
    installation uuid not null references android_push_private.devices(installation) on delete cascade,
    recipient uuid not null references public.perfiles(id) on delete cascade,
    attempts integer not null default 0,
    next_attempt timestamptz not null default now(),
    lease_id uuid,
    leased_until timestamptz,
    leased_token text,
    sent_at timestamptz,
    last_error text,
    created_at timestamptz not null default now(),
    unique(notification_id, installation)
);
alter table android_push_private.devices enable row level security;
alter table android_push_private.outbox enable row level security;
revoke all on all tables in schema android_push_private from public, anon, authenticated;
create index if not exists android_push_pending on android_push_private.outbox(next_attempt) where sent_at is null;

create or replace function public.register_android_push_device(p_installation uuid, p_token text)
returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null or not exists(select 1 from public.perfiles where id = auth.uid()) then
        raise exception 'Perfil requerido' using errcode = '42501';
    end if;
    if p_installation is null or p_token is null or length(p_token) not between 20 and 4096 then
        raise exception 'Dispositivo inválido' using errcode = '22023';
    end if;
    perform pg_advisory_xact_lock(hashtextextended(p_token, 0));
    delete from android_push_private.devices where token = p_token and installation <> p_installation;
    delete from android_push_private.outbox where installation = p_installation and recipient <> auth.uid();
    insert into android_push_private.devices(installation, perfil_id, token)
    values(p_installation, auth.uid(), p_token)
    on conflict(installation) do update set perfil_id = excluded.perfil_id, token = excluded.token, updated_at = now();
end;
$$;

create or replace function public.unregister_android_push_device(p_installation uuid)
returns void language sql security definer set search_path = '' as $$
    delete from android_push_private.devices where installation = p_installation and perfil_id = auth.uid();
$$;

create or replace function android_push_private.enqueue()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    insert into android_push_private.outbox(notification_id, installation, recipient)
    select new.id, d.installation, new.perfil_id
    from android_push_private.devices d where d.perfil_id = new.perfil_id
    on conflict(notification_id, installation) do nothing;
    return new;
end;
$$;
drop trigger if exists android_notification_push on public.notificaciones_app;
create trigger android_notification_push after insert on public.notificaciones_app
for each row execute function android_push_private.enqueue();

create or replace function public.claim_android_notification_pushes()
returns table(job_id uuid, lease uuid, notification_id uuid, recipient uuid, token text, title text, body text)
language plpgsql security definer set search_path = '' as $$
begin
    delete from android_push_private.outbox where created_at < now() - interval '7 days';
    return query
    with candidates as (
        select o.id from android_push_private.outbox o
        join android_push_private.devices d on d.installation = o.installation and d.perfil_id = o.recipient
        join public.notificaciones_app n on n.id = o.notification_id and n.perfil_id = o.recipient
        where o.sent_at is null and o.attempts < 8 and o.next_attempt <= now()
          and (o.leased_until is null or o.leased_until < now())
          and o.created_at > now() - interval '24 hours'
        order by o.created_at limit 50 for update of o skip locked
    ), leased as (
        update android_push_private.outbox o
        set attempts = o.attempts + 1, lease_id = gen_random_uuid(), leased_until = now() + interval '5 minutes',
            leased_token = (select d.token from android_push_private.devices d where d.installation = o.installation)
        where o.id in (select c.id from candidates c) returning o.*
    )
    select l.id, l.lease_id, l.notification_id, l.recipient, l.leased_token, n.titulo, n.mensaje
    from leased l join public.notificaciones_app n on n.id = l.notification_id;
end;
$$;

create or replace function public.finish_android_notification_push(
    p_job uuid, p_lease uuid, p_success boolean, p_error text, p_invalid boolean default false
)
returns void language plpgsql security definer set search_path = '' as $$
declare j android_push_private.outbox%rowtype;
begin
    select * into j from android_push_private.outbox where id = p_job and lease_id = p_lease and sent_at is null for update;
    if not found then return; end if;
    if p_invalid then
        delete from android_push_private.devices
        where installation = j.installation and perfil_id = j.recipient and token = j.leased_token;
    end if;
    update android_push_private.outbox set
        sent_at = case when p_success then now() else null end,
        last_error = case when p_success then null else left(p_error, 120) end,
        lease_id = null, leased_until = null,
        next_attempt = now() + make_interval(secs => least(3600, (power(2, j.attempts) * 15)::integer))
    where id = j.id;
end;
$$;
revoke all on function public.register_android_push_device(uuid,text), public.unregister_android_push_device(uuid) from public, anon;
grant execute on function public.register_android_push_device(uuid,text), public.unregister_android_push_device(uuid) to authenticated;
revoke all on function android_push_private.enqueue() from public, anon, authenticated;
revoke all on function public.claim_android_notification_pushes(), public.finish_android_notification_push(uuid,uuid,boolean,text,boolean) from public, anon, authenticated;
grant execute on function public.claim_android_notification_pushes(), public.finish_android_notification_push(uuid,uuid,boolean,text,boolean) to service_role;
create or replace function public.check_account_access()
returns void language plpgsql stable security definer set search_path = '' as $$
declare path text := trim(both '/' from coalesce(current_setting('request.path',true),''));
begin
    if auth.role() = 'authenticated' and not public.account_is_approved() then
        if path in ('rpc/my_account_access','rpc/delete_my_account',
            'rpc/register_push_device','rpc/unregister_push_device',
            'rpc/register_android_push_device','rpc/unregister_android_push_device') then return; end if;
        if path = 'perfiles' and current_setting('request.method',true) in ('GET','HEAD') then return; end if;
        raise exception 'Tu cuenta no tiene acceso aprobado' using errcode = '42501';
    end if;
end;
$$;
notify pgrst, 'reload schema';
commit;
