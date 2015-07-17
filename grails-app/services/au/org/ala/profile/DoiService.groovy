package au.org.ala.profile

import au.org.ala.web.AuthService
import groovy.xml.MarkupBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.commons.httpclient.HttpStatus

import java.text.SimpleDateFormat

class DoiService {

    static final String DATA_CITE_XSD_VERSION = "3"
    static final String DATA_CITE_XSD = "http://schema.datacite.org/meta/kernel-${DATA_CITE_XSD_VERSION}/metadata.xsd"

    static final String ANDS_RESPONSE_STATUS_OK = "MT090"
    static final String ANDS_RESPONSE_STATUS_DEAD = "MT091"
    static final String ANDS_RESPONSE_MINT_SUCCESS = "MT001"

    AuthService authService
    def grailsApplication

    Map serviceStatus() {
        String andsUrl = "${grailsApplication.config.ands.doi.service.url}status.json"

        Map status = [:]
        try {
            def response = new RESTClient(andsUrl).get(requestContentType: ContentType.JSON, contentType: ContentType.JSON)
            if ((response.status as int) == HttpStatus.SC_OK) {
                status.statusCode = response?.data?.response.responsecode
                status.message = "${response?.data?.response.message} - ${response?.data?.response.verbosemessage}"
            } else {
                status.statusCode = response.status
                status.message = HttpStatus.getStatusText(response.status)
            }
        } catch (Exception e) {
            status.statusCode = "E001"
            status.message = e.getMessage()
            log.error "DOI Service health check failed", e
        }

        status
    }

    /**
     * ANDS service documentation can be found here: http://ands.org.au/services/cmd-technical-document.pdf
     *
     * @param opus
     * @param publication
     * @return
     */
    Map mintDOI(Opus opus, Publication publication) {
        Map result = [:]

        Map andsServiceStatus = serviceStatus()
        if (andsServiceStatus.statusCode == ANDS_RESPONSE_STATUS_OK) {
            log.debug "Requesting new DOI from ANDS..."
            // The ANDS URL must have a trailing slash or you get an empty response back
            String andsUrl = "${grailsApplication.config.ands.doi.service.url}mint.json/"
            String appId = "${grailsApplication.config.ands.doi.app.id}"
            String requestXml = buildXml(opus, publication)
            log.debug requestXml

            String secret = "${appId}:${grailsApplication.config.ands.doi.key}".encodeAsBase64()

            String url = "${grailsApplication.config.doi.resolution.url.prefix}${publication.uuid}"
            Map query = [app_id: "${appId}", url: url]
            Map headers = [Accept: ContentType.JSON, Authorization: "Basic ${secret}"]

            RESTClient client = new RESTClient(andsUrl)
            def response = client.post(headers: headers,
                    query: query,
                    requestContentType: ContentType.URLENC,
                    contentType: ContentType.JSON,
                    body: [xml: requestXml])


            if (response.status as int == HttpStatus.SC_OK) {
                def json = response.data
                log.debug "DOI response = ${json}"

                if (json.response.responsecode == ANDS_RESPONSE_MINT_SUCCESS) {
                    log.debug "Minted new doi ${json.response.doi}"
                    result.status = "success"
                    result.doi = json.response.doi
                } else {
                    result.status = "error"
                    result.errorCode = json.response.responsecode
                    result.error = "${json.response.message}: ${json.response.verbosemessage}"

                    log.error("Failed to mint new doi: ${json.response.responsecode} - ${json.response.message}: ${json.response.verbosemessage}")
                }
            } else {
                result.status = "error"
                result.errorCode = response.status
                result.error = HttpStatus.getStatusText(response.status)
            }
        } else {
            result.status = "error"
            result.errorCode = ANDS_RESPONSE_STATUS_DEAD
            result.error = "The ANDS DOI minting service is not available."
            log.error "The ANDS DOI minting service is not available: ${andsServiceStatus}"
        }

        return result
    }

    /**
     * Schema documentation can be found here: https://schema.datacite.org/meta/kernel-3/doc/DataCite-MetadataKernel_v3.1.pdf
     *
     * @param opus
     * @param publication
     * @return
     */
    private String buildXml(Opus opus, Publication publication) {
        StringWriter writer = new StringWriter()

        MarkupBuilder xml = new MarkupBuilder(writer)

        xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        xml.resource(xmlns: "http://datacite.org/schema/kernel-${DATA_CITE_XSD_VERSION}",
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation": "http://datacite.org/schema/kernel-${DATA_CITE_XSD_VERSION} ${DATA_CITE_XSD}") {
            identifier(identifierType: "DOI", "10.5072/example")
            creators() {
                creator() {
                    creatorName(publication.authors)
                }
            }
            titles() {
                title("${publication.title}")
                title(titleType: "Subtitle", "Version ${publication.version}")
            }
            publisher(opus.title)
            publicationYear(Calendar.getInstance().get(Calendar.YEAR))
            subjects() {
                subject(publication.title)
            }
            contributors() {
                contributor(contributorType: "Editor") {
                    contributorName(authService.getUserForUserId(authService.getUserId()).displayName)
                }
            }
            dates() {
                date(dateType: "Created", new SimpleDateFormat("yyyy-MM-dd").format(publication.publicationDate))
            }
            language("en")
            resourceType(resourceTypeGeneral: "Text", "Species information")
            descriptions() {
                description(descriptionType: "Other", "Taxonomic treatment for ${publication.title}")
            }
        }

        writer.toString()
    }
}
