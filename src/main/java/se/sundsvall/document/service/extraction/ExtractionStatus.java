package se.sundsvall.document.service.extraction;

public enum ExtractionStatus {

	/** Tika produced text that was persisted. */
	SUCCESS,

	/** Tika ran but threw (corrupt/encrypted file). File is still stored; search by content won't hit. */
	FAILED,

	/** Tika detected a format we don't extract (image, binary, archive without indexable contents). */
	UNSUPPORTED,

	/** Backfill marker for rows that existed before V1_9; the mapper never writes this. */
	PENDING_REINDEX
}
