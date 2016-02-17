package au.org.ala.profile

import au.org.ala.profile.api.ExportController
import org.apache.http.HttpStatus

class ExportControllerSpec extends BaseIntegrationSpec {

    ExportController controller = new ExportController()

    def "export should only export profiles from the specified collection"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "a", opus: opus1)
        save new Profile(scientificName: "b", opus: opus2)

        when: "asking for 2 records with an offset of 1"
        controller.params.opusId = opus2.uuid
        controller.params.includeArchived = true
        controller.exportCollection()

        then: "the results should be [b, c] (i.e. [profile2, profile3]"
        controller.response.status == HttpStatus.SC_OK
        controller.response.json.profilesInCollection == 1
        controller.response.json.profiles.size() == 1
        controller.response.json.profiles[0].name == "b"
    }

    def "export should not include archived profiles by default"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "a", opus: opus, archivedDate: new Date())
        save new Profile(scientificName: "b", opus: opus)

        when: "asking for 2 records with an offset of 1"
        controller.params.opusId = opus.uuid
        controller.exportCollection()

        then: "the results should be [b, c] (i.e. [profile2, profile3]"
        controller.response.status == HttpStatus.SC_OK
        controller.response.json.profilesInCollection == 1
        controller.response.json.profiles.size() == 1
        controller.response.json.profiles[0].name == "b"
    }

    def "export should include archived profiles when includeArchived = true"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "a", opus: opus, archivedDate: new Date())
        save new Profile(scientificName: "b", opus: opus)

        when: "asking for 2 records with an offset of 1"
        controller.params.opusId = opus.uuid
        controller.params.includeArchived = true
        controller.exportCollection()

        then: "the results should be [b, c] (i.e. [profile2, profile3]"
        controller.response.status == HttpStatus.SC_OK
        controller.response.json.profilesInCollection == 2
        controller.response.json.profiles.size() == 2
        controller.response.json.profiles[0].name == "a"
        controller.response.json.profiles[1].name == "b"
    }

    def "export should only retrieve the max number of profiles, starting from the specified offset"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "a", opus: opus)
        save new Profile(scientificName: "b", opus: opus)
        save new Profile(scientificName: "c", opus: opus)
        save new Profile(scientificName: "d", opus: opus)

        when: "asking for 2 records with an offset of 1"
        controller.params.opusId = opus.uuid
        controller.params.max = 2
        controller.params.offset = 1
        controller.exportCollection()

        then: "the results should be [b, c] (i.e. [profile2, profile3]"
        controller.response.status == HttpStatus.SC_OK
        controller.response.json.profilesInCollection == 4
        controller.response.json.profiles.size() == 2
        controller.response.json.profiles[0].name == "b"
        controller.response.json.profiles[1].name == "c"
    }
}
