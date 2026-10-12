begin;

create or replace function public.claim_android_pushes_v2()
returns table(job_id uuid, lease uuid, notification_id uuid, recipient uuid, token text,
  title text, body text, is_alarm boolean)
language sql security definer set search_path='' as $$
  select j.*,coalesce(n.payload->>'is_alarm','false')='true'
  from public.claim_android_pushes() j
  join public.notificaciones_app n on n.id=j.notification_id and n.perfil_id=j.recipient;
$$;

revoke all on function public.claim_android_pushes_v2() from public,anon,authenticated;
grant execute on function public.claim_android_pushes_v2() to service_role;

notify pgrst,'reload schema';
commit;
