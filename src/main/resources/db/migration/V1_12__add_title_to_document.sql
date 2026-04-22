alter table document
    add column title varchar(255) not null default '';

-- The default is only there to backfill existing rows during the column addition. Every new
-- revision is written by the JPA mapper which always provides a title (request DTO enforces
-- @NotBlank), so drop the default to prevent accidental blank inserts via raw SQL.
alter table document
    alter column title drop default;
