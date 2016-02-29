package au.org.ala.profile.util

import au.org.ala.profile.Attachment
import au.org.ala.profile.Attribute
import au.org.ala.profile.Authorship
import au.org.ala.profile.Bibliography
import au.org.ala.profile.Classification
import au.org.ala.profile.Contributor
import au.org.ala.profile.DraftProfile
import au.org.ala.profile.Link
import au.org.ala.profile.Profile
import au.org.ala.profile.Publication
import au.org.ala.profile.Term
import spock.lang.Specification

class DraftUtilTest extends Specification {

    Profile original
    Contributor fred
    Contributor jill
    Contributor bob
    Attribute attribute1
    Attribute attribute2

    def setup() {
        fred = new Contributor(uuid: "user1", userId: "user1", name: "fred")
        jill = new Contributor(uuid: "user2", userId: "user2", name: "jill")
        bob = new Contributor(uuid: "user3", userId: "user3", name: "bob")
        attribute1 = new Attribute(uuid: "attr1", title: new Term(name: "attr1"), text: "attribute1", creators: [fred], editors: [jill, bob])
        attribute2 = new Attribute(uuid: "attr2", title: new Term(name: "attr2"), text: "attribute2", creators: [jill], editors: [fred])
        original = new Profile(
                uuid: "uuid",
                scientificName: "sciName",
                nameAuthor: "nameAuthor",
                guid: "guid",
                rank: "rank",
                taxonomyTree: "taxonomyTree",
                nslNameIdentifier: "nslId",
                primaryImage: "primaryImage",
                imageDisplayOptions: [image1: ImageOption.EXCLUDE, image2: ImageOption.EXCLUDE],
                specimenIds: ["spec1", "spec2"],
                authorship: [new Authorship(category: new Term(name: "category1"), text: "bob"), new Authorship(category: new Term(name: "category2"), text: "jill")],
                classification: [new Classification(rank: "kingdom", name: "Plantae"), new Classification(rank: "family", name: "Acacia")],
                links: [new Link(title: "link1"), new Link(title: "link2")],
                bhlLinks: [new Link(title: "bhl1"), new Link(title: "bhl2")],
                bibliography: [new Bibliography(text: "bib1"), new Bibliography(text: "bib2")],
                publications: [new Publication(title: "pub1"), new Publication(title: "pub2")],
                attributes: [attribute1, attribute2],
                attachments: [new Attachment(title: "doc1"), new Attachment(title: "doc2")],
                dateCreated: new Date()
        )
    }

    def "createDraft should copy all fields"() {
        when:
        DraftProfile draft = DraftUtil.createDraft(original)

        then:
        draft.uuid == original.uuid
        draft.scientificName == original.scientificName
        draft.nameAuthor == original.nameAuthor
        draft.rank == original.rank
        draft.guid == original.guid
        draft.nslNameIdentifier == original.nslNameIdentifier
        draft.primaryImage == original.primaryImage
        draft.taxonomyTree == original.taxonomyTree

        !draft.imageDisplayOptions.is(original.imageDisplayOptions)
        draft.imageDisplayOptions == original.imageDisplayOptions

        !draft.specimenIds.is(original.specimenIds)
        draft.specimenIds == original.specimenIds

        !draft.authorship.is(original.authorship)
        draft.authorship == original.authorship

        !draft.classification.is(original.classification)
        draft.classification == original.classification

        !draft.links.is(original.links)
        draft.links == original.links

        !draft.bhlLinks.is(original.bhlLinks)
        draft.bhlLinks == original.bhlLinks

        !draft.bibliography.is(original.bibliography)
        draft.bibliography == original.bibliography

        !draft.publications.is(original.publications)
        draft.publications == original.publications

        !draft.attributes.is(original.attributes)
        draft.attributes == original.attributes as List

        !draft.attachments.is(original.attachments)
        draft.attachments == original.attachments as List
    }

    def "cloneAuthorship should create a new object with copies of all attributes"() {
        given:
        Authorship original = new Authorship(category: new Term(name: "cat1"), text: "text1")

        when:
        Authorship clone = DraftUtil.cloneAuthorship(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "clone classification should create a new object with copies of all attributes"() {
        given:
        Classification original = new Classification(name: "name", rank: "rank", guid: "guid")

        when:
        Classification clone = DraftUtil.cloneClassification(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "cloneBibliography should create a new object with copies of all attributes"() {
        given:
        Bibliography original = new Bibliography(uuid: "uuid", text: "text", order: 1)

        when:
        Bibliography clone = DraftUtil.cloneBibliography(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "cloneLink should create a new object with copies of all attributes"() {
        given:
        Link original = new Link(uuid: "uuid", url: "url", title: "title", description: "description", doi: "doi", edition: "edition", publisherName: "publisher", fullTitle: "fullTitle")

        when:
        Link clone = DraftUtil.cloneLink(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "clonePublication should create a new object with copies of all attributes"() {
        given:
        Publication original = new Publication(uuid: "uuid", publicationDate: new Date(), title: "title", doi: "doi", userId: "userId", authors: "authors")

        when:
        Publication clone = DraftUtil.clonePublication(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "cloneAttachment should create a new object with copies of all attributes"() {
        given:
        Attachment original = new Attachment(uuid: "uuid", title: "title", description: "description", rights: "rights", rightsHolder: "rightsHolder", licence: "licence", creator: "creator", createdDate: new Date())

        when:
        Attachment clone = DraftUtil.cloneAttachment(original)

        then:
        !clone.is(original)
        clone == original
    }

    def "cloneAttribute should create a new object with copies of text fields, but the ORIGINAL references"() {
        given:
        Term term = new Term(name: "title")
        Attribute original = new Attribute(uuid: "uuid", text: "text", id: 1L, title: term, original: attribute1, creators: [fred, jill], editors: [bob, jill])

        when:
        Attribute clone = DraftUtil.cloneAttribute(original)

        then:
        !clone.is(original)
        clone.uuid == original.uuid
        clone.text == original.text
        clone.id == original.id
        clone.title.is(original.title)
        clone.original.is(original.original)
        !clone.creators.is(original.creators)
        clone.creators[0].is(jill)
        clone.creators[1].is(fred)
        !clone.editors.is(original.editors)
        clone.editors[0].is(jill)
        clone.editors[1].is(bob)
    }
}
