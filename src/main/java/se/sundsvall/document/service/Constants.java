package se.sundsvall.document.service;

public final class Constants {

	private Constants() {}

	// Templates
	public static final String TEMPLATE_EVENTLOG_MESSAGE_CONFIDENTIALITY_UPDATED_ON_DOCUMENT = "Confidentiality flag updated to: '%s' with legal citation: '%s' for document with registrationNumber: '%s'. Action performed by: '%s'";
	public static final String TEMPLATE_EVENTLOG_MESSAGE_RESPONSIBILITIES_UPDATED_ON_DOCUMENT = "Responsibilities updated from: '%s' to: '%s' for document with registrationNumber: '%s'. Action performed by: '%s'";
	public static final String TEMPLATE_REGISTRATION_NUMBER = "%s-%s-%s"; // [YYYY-MUNICIPALITY_ID-SEQUENCE]

	// Error messages
	public static final String ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND = "No document with registrationNumber: '%s' could be found!";
	public static final String ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND = "No document with registrationNumber: '%s' and revision: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_NOT_FOUND = "No document file for registrationNumber: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_AND_REVISION_NOT_FOUND = "No document file content with registrationNumber: '%s' and revision: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_ID_NOT_FOUND = "No document file content with ID: '%s' could be found!";
	public static final String ERROR_DOCUMENT_FILE_BY_REGISTRATION_NUMBER_COULD_NOT_READ = "Could not read file content for document data with ID: '%s'!";

	// API documentation
	public static final String SEARCH_DOCUMENTATION = """
		Parameters:
		- includeConfidential: Should the search include confidential documents? Datatype - boolean (default: false)
		- boolean onlyLatestRevision: Should the search include only the latest revision of the documents? Datatype - boolean (default: false)
		- query: Search query. Allows asterisk (*) as wildcard. Datatype - String

		The search query is used to match in the following fields using a LIKE-TO-LOWER-CASE comparison:
		- createdBy
		- description
		- municipalityId
		- registrationNumber
		- fileName
		- mimeType
		- metadataKey
		- metadataValue

		""";

	public static final String SEARCH_BY_PARAMETERS_DOCUMENTATION = """
		Parameters:
		- createdBy: Filter by the user that created the document. Datatype - String
		- includeConfidential: Should the search include confidential documents? Datatype - boolean (default: false)
		- onlyLatestRevision: Should the search include only the latest revision of the documents? Datatype - boolean (default: false)
		- documentTypes: Which document types to include in the search. Datatype - List of Strings
		- metaData: Uses the metadata object to search for documents with specific metadata. Datatype - List of metadata objects.
		- responsibilities: Uses document responsibilities to search for documents where at least one username matches. Usernames are case-insensitive and stored lowercased. Datatype - List of DocumentResponsibility objects.
		- page: The page number to retrieve. Datatype - integer (default: 1)
		- limit: The number of documents to retrieve per page. Datatype - integer (default: 100)

		Objects:
		- MetaData: {
			- key: A given metadata key, this is optional. All metadata will be searched if key is not provided. Datatype - String
			- matchesAny: Returns documents where metadata entry with the given key have at least one of the matchesAny values (if key is present), or if the complete set of metadata have at least one of the matchesAny (when no key is present). Datatype - List of Strings
			- matchesAll: Returns documents where metadata entry with the given key have at least one of the matchesAny values (if key is present), or if the complete set of metadata have at least one of the matchesAny (when no key is present). Datatype - List of Strings
		}
		- DocumentResponsibility: {
			- username: Username. Case-insensitive; stored lowercased. Datatype - String
		}
		""";
}
