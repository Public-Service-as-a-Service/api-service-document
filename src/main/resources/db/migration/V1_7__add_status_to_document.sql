alter table document
    add column status varchar(20) not null default 'ACTIVE';

create index ix_status on document (status);

-- Default 'ACTIVE' was only for backfilling existing rows; from here on every insert
-- must supply a status explicitly (the mapper always sets DRAFT for new revisions).
alter table document alter column status drop default;
