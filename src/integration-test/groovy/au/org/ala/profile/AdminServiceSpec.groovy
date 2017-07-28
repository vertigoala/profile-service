package au.org.ala.profile

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback

@Integration
@Rollback
class AdminServiceSpec extends BaseIntegrationSpec {

    AdminService service = new AdminService()

    def setup() {

    }

    def "deleteTag should delete the corresponding tag from all Opuses"() {
        given:
        Tag tag1 = new Tag(uuid: "1", abbrev: "tag", name: "tag1", colour: "white")
        save tag1
        Tag tag2 = new Tag(uuid: "2", abbrev: "tag2", name: "tag2", colour: "white")
        save tag2
        save new Opus(uuid: "opus1", title: "opus1", dataResourceUid: "1", glossary: new Glossary(), tags: [tag1])
        save new Opus(uuid: "opus2", title: "opus2", dataResourceUid: "2", glossary: new Glossary(), tags: [tag2])
        save new Opus(uuid: "opus3", title: "opus3", dataResourceUid: "3", glossary: new Glossary(), tags: [tag1, tag2])

        when:
        service.deleteTag("1")

        then:
        Opus.findByUuid("opus1").tags.isEmpty()
        Opus.findByUuid("opus2").tags == [tag2] as Set
        Opus.findByUuid("opus3").tags == [tag2] as Set
        Tag.list().size() == 1
    }
}
