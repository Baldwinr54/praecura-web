create table if not exists system_settings (
  setting_key varchar(80) primary key,
  setting_value varchar(2000),
  updated_by varchar(120),
  updated_at timestamp not null default now()
);

create index if not exists idx_system_settings_updated_at
  on system_settings(updated_at);
