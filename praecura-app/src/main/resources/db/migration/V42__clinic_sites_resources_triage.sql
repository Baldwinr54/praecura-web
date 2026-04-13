create table if not exists clinic_sites (
  id bigserial primary key,
  name varchar(120) not null,
  code varchar(30),
  address varchar(200),
  phone varchar(30),
  notes varchar(500),
  active boolean not null default true
);

create table if not exists clinic_resources (
  id bigserial primary key,
  name varchar(120) not null,
  type varchar(30) not null,
  site_id bigint not null,
  notes varchar(500),
  active boolean not null default true,
  constraint fk_resources_site foreign key (site_id) references clinic_sites(id)
);

alter table appointments
  add column if not exists site_id bigint,
  add column if not exists resource_id bigint,
  add column if not exists triage_level varchar(20),
  add column if not exists triage_notes varchar(500),
  add column if not exists checked_in_at timestamp,
  add column if not exists started_at timestamp,
  add column if not exists completed_at timestamp;

alter table appointments
  add constraint fk_appointments_site foreign key (site_id) references clinic_sites(id);

alter table appointments
  add constraint fk_appointments_resource foreign key (resource_id) references clinic_resources(id);

create index if not exists idx_appointments_site on appointments(site_id);
create index if not exists idx_appointments_resource on appointments(resource_id);
