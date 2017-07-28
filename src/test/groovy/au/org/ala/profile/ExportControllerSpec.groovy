package au.org.ala.profile

import au.org.ala.profile.api.ExportController
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.apache.http.HttpStatus
import spock.lang.Specification

@TestFor(ExportController)
@Mock([Opus, Profile, Glossary])
class ExportControllerSpec extends Specification {

    def exportService

    def setup() {
        exportService = Mock(ExportService)
        controller.exportService = exportService
    }

    def "exportCollection should call the export service with the request params"() {
        setup:

        given:
        Opus opus2 = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when: "asking for summary of 2 records with an offset of 1 including archived"
        controller.params.opusId = opus2.uuid
        controller.params.includeArchived = 'true'
        controller.params.summary = 'true'
        controller.params.max = '2'
        controller.params.offset = '1'
        controller.exportCollection()

        then: "then the service should be called"
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportCollection(response.outputStream, opus2, 2, 1, true, true)
    }

    def "exportCollection should call the service with default params"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when: "asking for defaults"
        controller.params.opusId = opus.uuid
        controller.exportCollection()

        then: "the service should be called"
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportCollection(response.outputStream, opus, ExportController.DEFAULT_MAXIMUM_PAGE_SIZE, 0, false, false)
    }

    def "exportCollection should return not found when the opus doesn't exist"() {

        when: "asking for defaults"
        controller.params.opusId = 'abcd'
        controller.exportCollection()

        then: "the not found is returned"
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "exportCollection should return bad request when no opus id is given"() {

        when: "asking for defaults"
        controller.exportCollection()

        then: "the not found is returned"
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "getProfiles should retrieve profiles by scientificName OR by guid"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when: "asked for profiles with name 'a' or guid 'g2'"
        controller.params.opusIds = opus.uuid
        controller.params.profileNames = "a"
        controller.params.guids = "g2"
        controller.getProfiles()

        then: "export service is called"
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportProfiles(response.outputStream, [opus.uuid], [], ['a'], ['g2'], false)
    }

    def "getProfiles should handle multiple profileNames"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when:
        controller.params.opusIds = opus.uuid
        controller.params.profileNames = "a,d"
        controller.getProfiles()

        then:
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportProfiles(response.outputStream, [opus.uuid], [], ['a','d'], [], false)
    }

    def "getProfiles should handle multiple guids"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when:
        controller.params.opusIds = opus.uuid
        controller.params.guids = "g1,g3"
        controller.getProfiles()

        then:
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportProfiles(response.outputStream, [opus.uuid], [], [], ['g1','g3'], false)
    }

    def "getProfiles should handle multiple tags"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title").save()

        when:
        controller.params.opusIds = opus.uuid
        controller.params.guids = "g1,g3"
        controller.params.tags = 'a,b'
        controller.getProfiles()

        then:
        response.status == HttpStatus.SC_OK
        response.contentType == JSON_CONTENT_TYPE
        1 * exportService.exportProfiles(response.outputStream, [opus.uuid], ['a','b'], [], ['g1','g3'], false)
    }

}
