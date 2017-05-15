package au.org.ala.profile

import au.org.ala.web.AuthService
import au.org.ala.web.UserDetails
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.http.HttpStatus
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine


class DoiServiceSpec extends BaseIntegrationSpec {

    DoiService service = new DoiService()

    def setup() {
        service.grailsApplication = [config:
                                             [ands:
                                                      [
                                                              doi: [
                                                                      service: [url: "http://ands.bla.bla/"],
                                                                      app    : [id: "appId"],
                                                                      key    : "secretKey"],
                                                      ]
                                              ,
                                              doi : [
                                                      resolution: [
                                                              url: [prefix: "http://blabla/publication"]
                                                      ]
                                              ]
                                             ]
        ]
    }

    def mockServiceResponse(int statusResponseCode, Map statusJson, int mintResponseCode, Map mintJson) {
        RESTClient.metaClass.get { Map<String, ?> args ->
            BasicHttpResponse baseResponse
            HttpResponseDecorator decorator = null
            if (delegate.getUri().toString().endsWith("status.json")) {
                baseResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, statusResponseCode, "bla"))
                decorator = new HttpResponseDecorator(baseResponse, statusJson)
            } else {
                println "Unexpected service call"
            }

            decorator
        }

        RESTClient.metaClass.post { Map<String, ?> args ->
            BasicHttpResponse baseResponse
            HttpResponseDecorator decorator = null
            if (delegate.getUri().toString().endsWith("mint.json/")) {
                baseResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, mintResponseCode, "bla"))
                decorator = new HttpResponseDecorator(baseResponse, mintJson)
            } else {
                println "Unexpected service call"
            }

            decorator
        }
    }

    def "mintDoi should return an error result if the DOI service status is not OK"() {
        setup:
        service.authService = Mock(AuthService)
        service.authService.getUserId() >> "user1"
        service.authService.getUserForUserId(_) >> new UserDetails(userName: "username1")

        mockServiceResponse(HttpStatus.SC_OK, [response: [responsecode: DoiService.ANDS_RESPONSE_STATUS_DEAD, message: 'service is dead!!']], -1, null)

        when:

        Map result = service.mintDOI(new Opus(title: "Opus"), new Publication(authors: "fred", title: "species1", publicationDate: new Date(), version: 1))

        then:
        result.status == "error"
    }

    def "mintDoi should return an error if the HTTP status of the DOI service status check is not 200"() {
        setup:
        service.authService = Mock(AuthService)
        service.authService.getUserId() >> "user1"
        service.authService.getUserForUserId(_) >> new UserDetails(userId: 'user1', userName: "username1")

        mockServiceResponse(HttpStatus.SC_BAD_REQUEST, [response: [responsecode: DoiService.ANDS_RESPONSE_STATUS_OK, message: 'service is dead!!']], -1, null)

        when:

        Map result = service.mintDOI(new Opus(title: "Opus"), new Publication(authors: "fred", title: "species1", publicationDate: new Date(), version: 1))

        then:
        result.status == "error"
    }

    def "mintDoi should return an error if the HTTP status of the DOI mint service is not 200"() {
        setup:
        service.authService = Mock(AuthService)
        service.authService.getUserId() >> "user1"
        service.authService.getUserForUserId(_) >> new UserDetails(userId: 'user1', userName: "username1")

        mockServiceResponse(HttpStatus.SC_OK, [response: [responsecode: DoiService.ANDS_RESPONSE_STATUS_OK, message: 'all good!!']],
                HttpStatus.SC_BAD_GATEWAY, null)

        when:

        Map result = service.mintDOI(new Opus(title: "Opus"), new Publication(authors: "fred", title: "species1", publicationDate: new Date(), version: 1))

        then:
        result.status == "error"
    }

    def "mintDoi should return an error if the DOI mint service returns a response code other than the 'success' code"() {
        setup:
        service.authService = Mock(AuthService)
        service.authService.getUserId() >> "user1"
        service.authService.getUserForUserId(_) >> new UserDetails(userId: 'user1', userName: "username1")

        mockServiceResponse(HttpStatus.SC_OK, [response: [responsecode: DoiService.ANDS_RESPONSE_STATUS_OK, message: 'all good!!']],
                HttpStatus.SC_OK, [response: [responsecode: "xyz", message: 'it blew up again!!']])

        when:

        Map result = service.mintDOI(new Opus(title: "Opus"), new Publication(authors: "fred", title: "species1", publicationDate: new Date(), version: 1))

        then:
        result.status == "error"
    }

    def "mintDoi should return a the DOI if the DOI mint service returns the 'success' response code"() {
        setup:
        service.authService = Mock(AuthService)
        service.authService.getUserId() >> "user1"
        service.authService.getUserForUserId(_) >> new UserDetails(userId: 'user1', userName: "username1")

        mockServiceResponse(HttpStatus.SC_OK, [response: [responsecode: DoiService.ANDS_RESPONSE_STATUS_OK, message: 'all good!!']],
                HttpStatus.SC_OK, [response: [responsecode: DoiService.ANDS_RESPONSE_MINT_SUCCESS, doi: "12345"]])

        when:

        Map result = service.mintDOI(new Opus(title: "Opus"), new Publication(authors: "fred", title: "species1", publicationDate: new Date(), version: 1))

        then:
        result.status == "success"
        result.doi == "12345"
    }
}
