package au.org.ala.profile

import grails.test.mixin.TestFor

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProfileGroupService)
class ProfileGroupServiceSpec extends BaseIntegrationSpec {

    ProfileGroupService service = new ProfileGroupService()

    def setup() {
    }

    def cleanup() {
    }
//
//    def "save a group with features"() {
//        given:
//        Opus opus = new Opus(uuid: "opus1", title: "opus1", dataResourceUid: "1", glossary: new Glossary())
//        save opus
//
//        ProfileGroup = new ProfileGroup(uuid: "group1", language: "language1", englishName: "test")
//
//        language // or local name
//        String englishName
//
//    }
    def "createGroup should expect both arguments to be provided"() {
        when:
        service.createGroup(null, [a: "b"])

        then:
        thrown(IllegalArgumentException)

        when:
        service.createGroup("bla", [:])

        then:
        thrown(IllegalArgumentException)
    }

    def "createGroup should fail if the specified opus does not exist"() {
        when:
        service.createGroup("unknown", [a: "bla"])

        then:
        thrown(IllegalStateException)
    }

    def "createGroup should return the new profileGroup on success"() {
        given:
        Opus opus = new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")
        save opus

        when:
        ProfileGroup profileGroup = service.createGroup(opus.uuid, [language: "language1", englishName: "test"])

        then:
        profileGroup != null && profileGroup.id != null
        ProfileGroup.count() == 1
        ProfileGroup.list()[0].language == "language1"
    }
}
