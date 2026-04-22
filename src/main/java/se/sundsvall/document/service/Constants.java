package se.sundsvall.document.service;

public final class Constants {

	private Constants() {}

	// Templates
	public static final String TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT = "Confidentiality flag updated to: '%s' with legal citation: '%s' for document with registrationNumber: '%s'. Action performed by: '%s'";
	public static final String TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT = "Responsibilities updated from: '%s' to: '%s' for document with registrationNumber: '%s'. Action performed by: '%s'";
	public static final String TEMPLATE_EVENTLOG_MESSAGE_STATUS_UPDATED_ON_DOCUMENT = "Status changed from '%s' to '%s' for document with registrationNumber: '%s' and revision: '%s'. Action performed by: '%s'";
	public static final String TEMPLATE_REGISTRATION_NUMBER = "%s-%s-%s"; // [YYYY-MUNICIPALITY_ID-SEQUENCE]

	// Error messages
	public static final String ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND = "No document with registrationNumber: '%s' could be found!";
	public static final String ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND = "No document with registrationNumber: '%s' and revision: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND = "No document file for registrationNumber: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND = "No document file content with registrationNumber: '%s' and revision: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND = "No document file content with ID: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ = "Could not read file content for document data with ID: '%s'!";
	public static final String ERROR_NO_PUBLISHED_REVISION = "No published revision exists for document with registrationNumber: '%s'!";
	public static final String ERROR_STATUS_TRANSITION_NOT_ALLOWED = "Status transition from '%s' is not allowed for action '%s' on document with registrationNumber: '%s'!";
	public static final String ERROR_PUBLISH_EXPIRED = "Document with registrationNumber: '%s' is already expired (validTo=%s) - create a new revision to republish!";
	public static final String ERROR_VALID_FROM_AFTER_VALID_TO = "validFrom (%s) must not be after validTo (%s)!";
}
