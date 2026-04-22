package se.sundsvall.document.api;

public class Constants {

	private Constants() {}

	public static final String DOCUMENTS_BASE_PATH = "/{municipalityId}/documents";
	public static final String DOCUMENT_REVISIONS_BASE_PATH = "/{municipalityId}/documents/{registrationNumber}/revisions";
	public static final String DOCUMENT_STATISTICS_BASE_PATH = "/{municipalityId}/documents/{registrationNumber}/statistics";
	public static final String ADMIN_DOCUMENT_TYPES_BASE_PATH = "/{municipalityId}/admin/documenttypes";

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

	public static final String SEARCH_FILE_MATCHES_DOCUMENTATION = """
		Returns a stripped response: for each matching document, only the document ID plus the IDs
		and filenames of the files that actually matched the query. No document metadata,
		responsibilities, or other fields are included. Backed entirely by Elasticsearch — no
		database hydration.

		The query parameter is repeatable (1–10 entries). Each entry is phrase-matched independently
		and the results are combined with logical OR — a document matches if any of its files matches
		any of the supplied queries.

		Parameters:
		- includeConfidential: Should the search include confidential documents? Datatype - boolean (default: false)
		- onlyLatestRevision: Should the search include only the latest revision of the documents? Datatype - boolean (default: false)
		- query: One or more search queries (repeat the parameter to OR them). Datatype - List of String, min 1, max 10.

		""";

	public static final String SEARCH_BY_PARAMETERS_DOCUMENTATION = """
		Parameters:
		- createdBy: Filter by the user that created the document. Datatype - String
		- includeConfidential: Should the search include confidential documents? Datatype - boolean (default: false)
		- onlyLatestRevision: Should the search include only the latest revision of the documents? Datatype - boolean (default: false)
		- documentTypes: Which document types to include in the search. Datatype - List of Strings
		- statuses: Which lifecycle statuses to include. Datatype - List of DocumentStatus. If omitted, defaults to published statuses (SCHEDULED, ACTIVE, EXPIRED) - DRAFT and REVOKED are excluded. When set explicitly, the list is used as-is (admin may pass [DRAFT] to see only drafts).
		- metaData: Uses the metadata object to search for documents with specific metadata. Datatype - List of metadata objects.
		- responsibilities: Uses document responsibilities to search for documents where at least one personId matches. Datatype - List of DocumentResponsibility objects.
		- page: The page number to retrieve. Datatype - integer (default: 1)
		- limit: The number of documents to retrieve per page. Datatype - integer (default: 100)

		Objects:
		- MetaData: {
			- key: A given metadata key, this is optional. All metadata will be searched if key is not provided. Datatype - String
			- matchesAny: Returns documents where metadata entry with the given key have at least one of the matchesAny values (if key is present), or if the complete set of metadata have at least one of the matchesAny (when no key is present). Datatype - List of Strings
			- matchesAll: Returns documents where metadata entry with the given key have at least one of the matchesAny values (if key is present), or if the complete set of metadata have at least one of the matchesAny (when no key is present). Datatype - List of Strings
		}
		- DocumentResponsibility: {
			- personId: Person ID of the responsible party. Datatype - String (UUID)
		}
		""";
}
