-- Release D: marks rows created by the dev-only data import, so a whole import can be removed again.
-- Only nullable columns are added, so the previous version of the app keeps working on this schema.
ALTER TABLE supplier           ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE project            ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE child_part         ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE part_stage         ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE rejection          ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE machine_hour_entry ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE tool_item          ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE tool_resharpen     ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE rm_purchase        ADD COLUMN import_batch VARCHAR(60);
ALTER TABLE monthly_expense    ADD COLUMN import_batch VARCHAR(60);
