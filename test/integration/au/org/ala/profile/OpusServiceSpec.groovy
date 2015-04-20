package au.org.ala.profile

import au.org.ala.profile.security.Role

class OpusServiceSpec extends BaseIntegrationSpec {

    OpusService service = new OpusService()

    def setup() {

    }

    def "updateUsers should throw an IllegalArgumentException if no opus id or data are provided"() {
        when:
        service.updateUsers(null, [a: "b"])

        then:
        thrown IllegalArgumentException

        when:
        service.updateUsers("a", [:])

        then:
        thrown IllegalArgumentException
    }

    def "updateUsers should throw IllegalStateException if the opus does not exist"() {
        when:
        service.updateUsers("unknown", [a:"b"])

        then:
        thrown IllegalStateException
    }

    def "updateUsers should update the opus' authorities list"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUsers(opus.uuid, [authorities: [[userId: "user1", name: "Fred", role: "role_profile_admin", notes: "first note"],
                                                      [userId: "user2", name: "Bob", role: "role_profile_reviewer", notes: "second note"]]])

        then:
        Authority.count() == 2
        Contributor.count() == 2
        opus.authorities.size() == 2
        opus.authorities.find({it.user.userId == "user1"}).role == Role.ROLE_PROFILE_ADMIN
        opus.authorities.find({it.user.userId == "user2"}).role == Role.ROLE_PROFILE_REVIEWER
    }

    def "updateUsers should replace existing authorities list"() {
        given:
        Contributor user = new Contributor(userId: "user1", name: "jill")
        save user
        Authority auth = new Authority(user: user, role: Role.ROLE_PROFILE_ADMIN)
        save auth
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary(), authorities: [auth])
        save opus

        when:
        service.updateUsers(opus.uuid, [authorities: [[userId: "user2", name: "Fred", role: "role_profile_admin", notes: "first note"],
                                                      [userId: "user3", name: "Bob", role: "role_profile_reviewer", notes: "second note"]]])

        then:
        Authority.count() == 2
        Contributor.count() == 3
        opus.authorities.size() == 2
        opus.authorities.find({it.user.userId == "user2"}).role == Role.ROLE_PROFILE_ADMIN
        opus.authorities.find({it.user.userId == "user3"}).role == Role.ROLE_PROFILE_REVIEWER
    }

    def "updateUsers should fail if the name is not provided for a new user"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUsers(opus.uuid, [authorities: [[userId: "user1", role: "role_profile_admin", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }

    def "updateUsers should fail if the userId is not provided for a new user"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUsers(opus.uuid, [authorities: [[name: "fred", role: "role_profile_admin", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }

    def "updateUsers should fail if the role is not recognised"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUsers(opus.uuid, [authorities: [[userId: "user1", name: "fred", role: "unknown", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }
}
