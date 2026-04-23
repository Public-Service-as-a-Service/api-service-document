-- Index for efficient look-ups of documents that expire soon, used by:
--   * GET /documents/statistics (30-day expiring-soon window)
--   * DocumentStatusScheduler daily bulkExpire query
create index ix_municipality_id_valid_to
    on document (municipality_id, valid_to);
