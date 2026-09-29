-- Language of the guest review as detected by the model (ISO 639-1, e.g. "en", "fr"). Null for heuristic analyses.
alter table review_analyses add column if not exists language varchar(8);
