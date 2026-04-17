package se.sundsvall.document;

public class Constants {

	private Constants() {}

	public static final String DOCUMENTS_BASE_PATH = "/{municipalityId}/documents";
	public static final String DOCUMENT_REVISIONS_BASE_PATH = "/{municipalityId}/documents/{registrationNumber}/revisions";
	public static final String DOCUMENT_STATISTICS_BASE_PATH = "/{municipalityId}/documents/{registrationNumber}/statistics";
	public static final String ADMIN_DOCUMENT_TYPES_BASE_PATH = "/{municipalityId}/admin/documenttypes";
}
