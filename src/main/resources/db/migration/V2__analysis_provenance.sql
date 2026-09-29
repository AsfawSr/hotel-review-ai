-- Analysis provenance: which engine produced the result, and the model/prompt/policies used.
-- IF NOT EXISTS keeps this safe for databases where Hibernate ddl-auto already added the columns.

alter table review_analyses add column if not exists analysis_source varchar(20);
alter table review_analyses add column if not exists model_name varchar(100);
alter table review_analyses add column if not exists prompt_version varchar(40);
alter table review_analyses add column if not exists policy_context varchar(50000);
