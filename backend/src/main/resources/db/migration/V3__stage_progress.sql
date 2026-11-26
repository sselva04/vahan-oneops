-- Employees start and complete their own stages: remember when, and who completed it.
ALTER TABLE part_stage ADD COLUMN started_at   TIMESTAMP WITH TIME ZONE;
ALTER TABLE part_stage ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE part_stage ADD COLUMN completed_by BIGINT;
ALTER TABLE part_stage ADD CONSTRAINT fk_part_stage_completed_by FOREIGN KEY (completed_by) REFERENCES app_user (id);
