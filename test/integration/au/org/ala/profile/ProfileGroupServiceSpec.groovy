package au.org.ala.profile

import grails.test.mixin.TestFor

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
//@TestFor(ProfileGroupService)
class ProfileGroupServiceSpec extends BaseIntegrationSpec {

    ProfileGroupService service = new ProfileGroupService()

    def setup() {
    }

    def cleanup() {
    }

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
        ProfileGroup profileGroup = service.createGroup(opus.uuid, [language: "language1", description: "test", seasonMonths: 'Jan'])

        then:
        profileGroup != null && profileGroup.id != null
        ProfileGroup.count() == 1
        ProfileGroup.list()[0].language == "language1"
    }

    def "delete profile group should remove the group and profiles"() {
        given:
        Opus calendar = new Opus(glossary: new Glossary(), dataResourceUid: "dr5678", title: "calendar")
        save calendar

        ProfileGroup group = new ProfileGroup(opus: calendar, language: "language1", description: "test", seasonMonths: 'Jan')
        save group

        Profile profile1 = new Profile(uuid: "1", opus: calendar, scientificName: "sciName 1", group: group)
        Profile profile2 = new Profile(uuid: "2", opus: calendar, scientificName: "sciName 2", group: group)
        save profile1
        save profile2

        String groupId = group.uuid

        expect:
        ProfileGroup.count() == 1
        Profile.findByGroup(group).group.uuid == groupId

        when:
        service.deleteGroup(groupId)

        then:
        ProfileGroup.count() == 0
        Profile.findByGroup(group) == null
    }
}
