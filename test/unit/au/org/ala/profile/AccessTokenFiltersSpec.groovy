package au.org.ala.profile

import au.org.ala.profile.api.ExportController
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.FiltersUnitTestMixin
import org.apache.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(AccessTokenFilters)
@TestMixin([GrailsUnitTestMixin, FiltersUnitTestMixin, DomainClassUnitTestMixin])
@Unroll
@Mock([ExportController, ProfileController, Opus])
class AccessTokenFiltersSpec extends Specification {

    def "requests without an access token should be rejected"() {
        setup:
        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is no access token in the header"
        params.opusId = "abc"

        withFilters(controller: "export", action: "countProfiles") {
            Mock(ExportController).countProfiles()
        }

        then: "the request should be rejected"
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with an invalid access token should be rejected()"() {
        setup:
        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is an invalid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        withFilters(controller: "export", action: "countProfiles") {
            Mock(ExportController).countProfiles()
        }

        then: "the request should be rejected"
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with a valid access token should be accepted"() {
        setup:
        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "1234")

        withFilters(controller: "export", action: "countProfiles") {
            Mock(ExportController).countProfiles()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_OK
    }

    def "only controllers in the au.org.ala.profile.api package should be filtered"() {
        setup:
        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is no access token in the header but a controller in a non-api package is called"
        params.opusId = "abc"

        withFilters(controller: "status", action: "ping") {
            Mock(StatusController).ping()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_OK
    }
}