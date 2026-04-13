-- Add working schedule fields for doctors
ALTER TABLE doctors
  ADD COLUMN work_start TIME NOT NULL DEFAULT '08:00',
  ADD COLUMN work_end TIME NOT NULL DEFAULT '17:00',
  ADD COLUMN work_days VARCHAR(32) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI';
