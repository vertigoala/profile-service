package au.org.ala.profile

import au.org.ala.profile.security.Role
import au.org.ala.profile.util.ShareRequestStatus
import au.org.ala.web.AuthService
import grails.gsp.PageRenderer

class OpusServiceSpec extends BaseIntegrationSpec {

    OpusService service = new OpusService()
    EmailService emailService = Mock(EmailService)

    def setup() {
        service.authService = Mock(AuthService)
        service.authService.getUserForUserId(_) >> [displayName: "Fred"]
        service.authService.getUserId() >> "123"
        service.groovyPageRenderer = Mock(PageRenderer)
        service.groovyPageRenderer.render(_, _) >> "bla"
        service.emailService = emailService
    }

    def "updateUserAccess should throw an IllegalArgumentException if no opus id or data are provided"() {
        when:
        service.updateUserAccess(null, [a: "b"])

        then:
        thrown IllegalArgumentException

        when:
        service.updateUserAccess("a", [:])

        then:
        thrown IllegalArgumentException
    }

    def "updateUserAccess should throw IllegalStateException if the opus does not exist"() {
        when:
        service.updateUserAccess("unknown", [a:"b"])

        then:
        thrown IllegalStateException
    }

    def "updateUserAccess should update the opus' authorities list"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUserAccess(opus.uuid, [authorities: [[userId: "user1", name: "Fred", role: "role_profile_admin", notes: "first note"],
                                                      [userId: "user2", name: "Bob", role: "role_profile_reviewer", notes: "second note"]]])

        then:
        Authority.count() == 2
        Contributor.count() == 2
        opus.authorities.size() == 2
        opus.authorities.find({it.user.userId == "user1"}).role == Role.ROLE_PROFILE_ADMIN
        opus.authorities.find({it.user.userId == "user2"}).role == Role.ROLE_PROFILE_REVIEWER
    }

    def "updateUserAccess should replace existing authorities list"() {
        given:
        Contributor user = new Contributor(userId: "user1", name: "jill")
        save user
        Authority auth = new Authority(user: user, role: Role.ROLE_PROFILE_ADMIN)
        save auth
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary(), authorities: [auth])
        save opus

        when:
        service.updateUserAccess(opus.uuid, [authorities: [[userId: "user2", name: "Fred", role: "role_profile_admin", notes: "first note"],
                                                      [userId: "user3", name: "Bob", role: "role_profile_reviewer", notes: "second note"]]])

        then:
        Authority.count() == 2
        Contributor.count() == 3
        opus.authorities.size() == 2
        opus.authorities.find({it.user.userId == "user2"}).role == Role.ROLE_PROFILE_ADMIN
        opus.authorities.find({it.user.userId == "user3"}).role == Role.ROLE_PROFILE_REVIEWER
    }

    def "updateUserAccess should fail if the name is not provided for a new user"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUserAccess(opus.uuid, [authorities: [[userId: "user1", role: "role_profile_admin", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }

    def "updateUserAccess should fail if the userId is not provided for a new user"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUserAccess(opus.uuid, [authorities: [[name: "fred", role: "role_profile_admin", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }

    def "updateUserAccess should fail if the role is not recognised"() {
        given:
        Opus opus = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus

        when:
        service.updateUserAccess(opus.uuid, [authorities: [[userId: "user1", name: "fred", role: "unknown", notes: "first note"]]])

        then:
        thrown IllegalArgumentException
    }
    
    def "updateUserAccess should revoke all data sharing requests when the collection becomes private"() {
        given:
        Opus opus1 = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus1
        Opus opus2 = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary(), sharingDataWith: [new SupportingOpus(uuid: opus1.uuid, requestStatus: ShareRequestStatus.ACCEPTED)])
        save opus2

        opus1.supportingOpuses = [new SupportingOpus(uuid: opus2.uuid, requestStatus: ShareRequestStatus.ACCEPTED)]
        save opus1

        expect:
        opus2.sharingDataWith.size() == 1
        opus2.sharingDataWith.each {
            assert it.dateRejected == null
            assert it.requestStatus == ShareRequestStatus.ACCEPTED
        }

        when:
        service.updateUserAccess(opus2.uuid, [privateCollection: true])

        then:
        opus2.sharingDataWith.size() == 0
        opus1.supportingOpuses.size() == 1
        opus1.supportingOpuses.each {
            assert it.requestStatus == ShareRequestStatus.REVOKED
        }
    }

    def "updateUserAccess should not revoke all data sharing requests when the collection has not been changed to private"() {
        given:
        Opus opus1 = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary())
        save opus1
        Opus opus2 = new Opus(title: "opus", dataResourceUid: "123", glossary: new Glossary(), sharingDataWith: [new SupportingOpus(uuid: opus1.uuid, requestStatus: ShareRequestStatus.ACCEPTED)])
        save opus2

        opus1.supportingOpuses = [new SupportingOpus(uuid: opus2.uuid, requestStatus: ShareRequestStatus.ACCEPTED)]
        save opus1

        expect:
        opus2.sharingDataWith.size() == 1
        opus2.sharingDataWith.each {
            assert it.requestStatus == ShareRequestStatus.ACCEPTED
        }

        when:
        service.updateUserAccess(opus2.uuid, [privateCollection: false])

        then:
        opus2.sharingDataWith.size() == 1
        opus1.supportingOpuses.size() == 1
        opus1.supportingOpuses.each {
            assert it.requestStatus == ShareRequestStatus.ACCEPTED
        }
    }
}
