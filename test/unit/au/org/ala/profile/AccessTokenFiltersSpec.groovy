package au.org.ala.profile

import au.org.ala.profile.api.ExportController
import au.org.ala.profile.security.RequiresAccessToken
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

    def "requests without an access token should be rejected when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()
        
        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is no access token in the header"
        params.opusId = "abc"

        withFilters(controller: "annotatedClass", action: "action1") {
            controller.action1()
        }

        then: "the request should be rejected"
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with an invalid access token should be rejected when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()

        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is an invalid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        withFilters(controller: "annotatedClass", action: "action1") {
            controller.action1()
        }

        then: "the request should be rejected"
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with a valid access token should be accepted when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()

        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "1234")

        withFilters(controller: "annotatedClass", action: "action1") {
            controller.action1()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_OK
    }

    def "requests to a method annotated with RequiresAccessToken with a valid token should be accepted"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "1234")

        withFilters(controller: "annotatedMethod", action: "securedAction") {
            controller.securedAction()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_OK
    }

    def "requests to a method annotated with RequiresAccessToken with an invalid token should be rejected"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        withFilters(controller: "annotatedMethod", action: "securedAction") {
            controller.securedAction()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests to a method not annotated with RequiresAccessToken with an invalid token should be access"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        def mockOpus = mockFor(Opus)
        mockOpus.demand.static.findByUuid() { String uuid -> new Opus(uuid: UUID.randomUUID().toString(), accessToken: "1234") }

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        withFilters(controller: "annotatedMethod", action: "publicAction") {
            controller.publicAction()
        }

        then: "the request should be accepted"
        response.status == HttpStatus.SC_OK
    }

}

@RequiresAccessToken
class AnnotatedClassController {
    def action1() {
    }
}
class AnnotatedMethodController {
    @RequiresAccessToken
    def securedAction() {
    }
    def publicAction() {
    }
}