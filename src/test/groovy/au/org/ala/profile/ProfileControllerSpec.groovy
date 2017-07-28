package au.org.ala.profile

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ProfileController)
@Mock([Opus, Profile, DraftProfile, Attachment, Glossary, Name, ImageSettings, Attribute, Authorship, Classification, Link, Bibliography, Document, Publication, LocalImage, Attachment])
class ProfileControllerSpec extends Specification {

    ProfileService profileService

    def setup() {
        profileService = Mock(ProfileService)
        controller.profileService = profileService
        controller.attachmentService = Mock(AttachmentService)
        controller.opusService = Mock(OpusService)
        controller.opusService.isProfileOnMasterList(_, _) >> true
        controller.masterListService = Mock(MasterListService)
    }

    def "updateProfile should return the draft profile if it exists and the 'latest' query param is 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus, draft: [uuid: "123", scientificName: "draftSciName"]).save()

        when: "the latest param is 'true'"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.latest = "true"
        controller.updateProfile()

        then: "return the draft version"
        controller.response.status == 200
        controller.response.json.uuid == "123"
        controller.response.json.scientificName == "draftSciName"
    }

    def "updateProfile should return the public version of the profile if a draft exists but the 'latest' query param is not 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus, draft: [uuid: "123", scientificName: "draftSciName"]).save()

        when: "the latest param is not set"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.updateProfile()

        then: "return the public version"
        controller.response.status == 200
        controller.response.json.uuid == profile.uuid
        controller.response.json.scientificName == "sciName"
    }

    def "updateProfile should return the public profile if no draft exists, even if the 'latest' query param is 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus).save()

        when: "the latest param is 'true'"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.latest = "true"
        controller.updateProfile()

        then: "return the public version"
        controller.response.status == 200
        controller.response.json.uuid == profile.uuid
        controller.response.json.scientificName == "sciName"
    }

    def "getByUuid should return the draft profile if it exists and the 'latest' query param is 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus, draft: [uuid: "123", scientificName: "draftSciName"]).save()

        when: "the latest param is 'true'"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.latest = "true"
        controller.getByUuid()

        then: "return the draft version"
        controller.response.status == 200
        controller.response.json.uuid == "123"
        controller.response.json.scientificName == "draftSciName"
    }

    def "getByUuid should return the public version of the profile if a draft exists but the 'latest' query param is not 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus, draft: [uuid: "123", scientificName: "draftSciName"]).save()

        when: "the latest param is 'true'"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.getByUuid()

        then: "return the draft version"
        controller.response.status == 200
        controller.response.json.uuid == profile.uuid
        controller.response.json.scientificName == "sciName"
    }

    def "getByUuid should return the public profile if no draft exists, even if the 'latest' query param is 'true'"() {
        given:
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus).save()

        when: "the latest param is 'true'"
        controller.request.json = "{'a': 'b'}"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.latest = "true"
        controller.getByUuid()

        then: "return the draft version"
        controller.response.status == 200
        controller.response.json.uuid == profile.uuid
        controller.response.json.scientificName == "sciName"
    }

    def "downloadAttachment should use the draft if there is one and latest = true"() {
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus).save()
        profile.draft = new DraftProfile(uuid: profile.uuid, scientificName: profile.scientificName, attachments: [new Attachment(uuid: "1234")])
        profile.save()

        when:
        controller.params.latest = "true"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.attachmentId = "1234"
        controller.downloadAttachment()

        then:
        1 * controller.attachmentService.getAttachment("opusId", profile.uuid, "1234", _)
    }

    def "downloadAttachment should use the profile when latest = false even if there is a draft"() {
        Opus opus = new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1").save()
        Profile profile = new Profile(scientificName: "sciName", opus: opus, attachments: [new Attachment(uuid: "1234")]).save()
        profile.draft = new DraftProfile(uuid: profile.uuid, scientificName: profile.scientificName, attachments: [])
        profile.save()

        when:
        controller.params.latest = "false"
        controller.params.profileId = profile.uuid
        controller.params.opusId = "opusId"
        controller.params.attachmentId = "1234"
        controller.downloadAttachment()

        then:
        1 * controller.attachmentService.getAttachment("opusId", profile.uuid, "1234", _)
    }
}
