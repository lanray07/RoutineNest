create schema if not exists routinenest_private;
revoke all on schema routinenest_private from public, anon, authenticated;
create table routinenest_private.receipt_rate_buckets (
  bucket text primary key,
  requests integer not null check (requests > 0),
  expires_at timestamptz not null
);
alter table routinenest_private.receipt_rate_buckets enable row level security;
revoke all on routinenest_private.receipt_rate_buckets from public, anon, authenticated;
create function public.routinenest_take_receipt_slot(p_subject_hash text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  window_start bigint := floor(extract(epoch from clock_timestamp()) / 600)::bigint * 600;
  expires timestamptz;
  count_subject integer;
  count_global integer;
  retry integer;
begin
  if p_subject_hash is null or p_subject_hash !~ '^[0-9a-f]{64}$' then
    raise exception 'Invalid rate-limit subject';
  end if;
  expires := to_timestamp(window_start + 600);
  retry := greatest(1, ceil(extract(epoch from expires - clock_timestamp()))::integer);
  delete from routinenest_private.receipt_rate_buckets where expires_at < clock_timestamp() - interval '1 hour';
  insert into routinenest_private.receipt_rate_buckets values ('subject:' || p_subject_hash || ':' || window_start, 1, expires)
  on conflict (bucket) do update set requests = least(routinenest_private.receipt_rate_buckets.requests + 1, 61)
  returning requests into count_subject;
  if count_subject > 60 then
    return jsonb_build_object('allowed', false, 'retryAfterSeconds', retry);
  end if;
  insert into routinenest_private.receipt_rate_buckets values ('global:' || window_start, 1, expires)
  on conflict (bucket) do update set requests = least(routinenest_private.receipt_rate_buckets.requests + 1, 601)
  returning requests into count_global;
  return jsonb_build_object('allowed', count_global <= 600, 'retryAfterSeconds', retry);
end $$;
revoke all on function public.routinenest_take_receipt_slot(text) from public, anon, authenticated;
grant execute on function public.routinenest_take_receipt_slot(text) to service_role;
