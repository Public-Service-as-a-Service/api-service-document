package se.sundsvall.document.api.model;

public enum DocumentAccessType {

	/**
	 * The file was downloaded (Content-Disposition: attachment).
	 */
	DOWNLOAD,

	/**
	 * The file was viewed inline (Content-Disposition: inline) without being downloaded.
	 */
	VIEW
}
