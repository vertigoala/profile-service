package au.org.ala.profile

import au.org.ala.profile.security.RequiresAccessToken
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.web.InterceptorUnitTestMixin
import org.apache.http.HttpStatus
import org.grails.web.util.GrailsApplicationAttributes
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(AccessTokenInterceptor)
@TestMixin([GrailsUnitTestMixin, InterceptorUnitTestMixin, DomainClassUnitTestMixin])
@Unroll
@Mock(Opus)
class AccessTokenInterceptorSpec extends Specification {

    def setup() {
        new Opus(uuid: 'abc', accessToken: '1234', glossary: new Glossary(uuid: 'abc'), dataResourceUid: '123', title: 'title').save(false)
    }

    def "requests without an access token should be rejected when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()

        when: "there is no access token in the header"
        params.opusId = "abc"

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedClass')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'action1')
        withRequest(controllerName: 'annotatedClass', actionName: 'action1')
        def result = interceptor.before()

        then: "the request should be rejected"
        result == false
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with an invalid access token should be rejected when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()

        when: "there is an invalid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedClass')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'action1')
        withRequest(controllerName: "annotatedClass", actionName: "action1")
        def result = interceptor.before()

        then: "the request should be rejected"
        result == false
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests with a valid access token should be accepted when the controller class is annotated with RequiresAccessToken"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedClassController)
        AnnotatedClassController controller = new AnnotatedClassController()

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "1234")

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedClass')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'action1')
        withRequest(controllerName: "annotatedClass", actionName: "action1")
        def result = interceptor.before()

        then: "the request should be accepted"
        result == true
        response.status == HttpStatus.SC_OK
    }

    def "requests to a method annotated with RequiresAccessToken with a valid token should be accepted"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "1234")

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'securedAction')
        withRequest(controllerName: "annotatedMethod", actionName: "securedAction")
        def result = interceptor.before()

        then: "the request should be accepted"
        result == true
        response.status == HttpStatus.SC_OK
    }

    def "requests to a method annotated with RequiresAccessToken with an invalid token should be rejected"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'securedAction')
        withRequest(controllerName: "annotatedMethod", actionName: "securedAction")
        def result = interceptor.before()

        then: "the request should be accepted"
        result == false
        response.status == HttpStatus.SC_FORBIDDEN
    }

    def "requests to a method not annotated with RequiresAccessToken with an invalid token should be access"() {
        setup:
        // need to do this because grailsApplication.controllerClasses is empty in the filter when run from the unit test
        // unless we manually add the dummy controller class used in this test
        grailsApplication.addArtefact("Controller", AnnotatedMethodController)
        AnnotatedMethodController controller = new AnnotatedMethodController()

        when: "there is a valid access token in the header"
        params.opusId = "abc"
        request.addHeader("ACCESS-TOKEN", "garbage")

        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'annotatedMethod')
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'publicAction')
        withRequest(controllerName: "annotatedMethod", actionName: "publicAction")
        def result = interceptor.before()

        then: "the request should be accepted"
        result == true
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