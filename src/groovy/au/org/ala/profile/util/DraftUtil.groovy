package au.org.ala.profile.util

import au.org.ala.profile.Attribute
import au.org.ala.profile.Authorship
import au.org.ala.profile.Bibliography
import au.org.ala.profile.Classification
import au.org.ala.profile.DraftProfile
import au.org.ala.profile.Link
import au.org.ala.profile.Name
import au.org.ala.profile.Profile
import au.org.ala.profile.Publication

class DraftUtil {

    static void updateProfileFromDraft(Profile profile) {
        if (!profile.draft) {
            return
        }

        profile.uuid = profile.draft.uuid
        profile.scientificName = profile.draft.scientificName
        profile.nameAuthor = profile.draft.nameAuthor
        profile.fullName = profile.draft.fullName
        profile.matchedName = cloneName(profile.draft.matchedName)
        profile.rank = profile.draft.rank
        profile.nslNameIdentifier = profile.draft.nslNameIdentifier
        profile.nslNomenclatureIdentifier = profile.draft.nslNomenclatureIdentifier
        profile.primaryImage = profile.draft.primaryImage
        profile.excludedImages = profile.draft.excludedImages
        profile.specimenIds = profile.draft.specimenIds
        profile.authorship = profile.draft.authorship
        profile.classification = profile.draft.classification
        profile.links = profile.draft.links
        profile.bhlLinks = profile.draft.bhlLinks
        profile.bibliography = profile.draft.bibliography
        profile.publications = profile.draft.publications

        // Update the existing record rather than replacing it with the draft object,
        // otherwise, the hibernate dirty check will fail (dirty check looks for changes since the entity
        // was read from the DB, but the draft object is not treated as an entity (because it is an
        // embedded Mongo subdocument))
        profile.draft.attributes.each {
            String uuid = it.uuid
            Attribute existing = profile.attributes.find { it.uuid == uuid }
            if (existing) {
                existing.title = it.title
                existing.text = it.text
            } else {
                it.profile = profile
                profile.addToAttributes(it)
            }
        }
    }

    static DraftProfile createDraft(Profile profile) {
        DraftProfile clone = new DraftProfile()
        clone.draftDate = new Date()
        clone.uuid = profile.uuid
        clone.scientificName = profile.scientificName
        clone.nameAuthor = profile.nameAuthor
        clone.fullName = profile.fullName
        clone.matchedName = cloneName(profile.matchedName)
        clone.rank = profile.rank
        clone.guid = profile.guid
        clone.nslNameIdentifier = profile.nslNameIdentifier
        clone.nslNomenclatureIdentifier = profile.nslNomenclatureIdentifier
        clone.primaryImage = profile.primaryImage
        clone.excludedImages = profile.excludedImages?.collect()
        clone.specimenIds = profile.specimenIds?.collect()
        clone.authorship = profile.authorship?.collect { cloneAuthorship(it) }
        clone.classification = profile.classification?.collect { cloneClassification(it) }
        clone.links = profile.links?.collect { cloneLink(it) }
        clone.bhlLinks = profile.bhlLinks?.collect { cloneLink(it) }
        clone.bibliography = profile.bibliography?.collect { cloneBibliography(it) }
        clone.publications = profile.publications?.collect { clonePublication(it) }
        clone.attributes = profile.attributes?.collect { cloneAttribute(it) }

        clone.dateCreated = profile.dateCreated

        clone
    }

    static Name cloneName(Name source) {
        Name clone = null
        if (source != null) {
            clone = new Name()
            clone.scientificName = source.scientificName
            clone.nameAuthor = source.nameAuthor
            clone.fullName = source.fullName
            clone.guid = source.guid
        }
        clone
    }

    static Authorship cloneAuthorship(Authorship source) {
        Authorship clone = new Authorship()
        clone.category = source.category
        clone.text = source.text

        clone
    }

    static Classification cloneClassification(Classification source) {
        Classification clone = new Classification()
        clone.name = source.name
        clone.guid = source.guid
        clone.rank = source.rank

        clone
    }

    static Bibliography cloneBibliography(Bibliography source) {
        Bibliography clone = new Bibliography()
        clone.uuid = source.uuid
        clone.text = source.text
        clone.order = source.order

        clone
    }

    static Link cloneLink(Link source) {
        Link clone = new Link()
        clone.uuid = source.uuid
        clone.url = source.url
        clone.title = source.title
        clone.description = source.description
        clone.doi = source.doi
        clone.edition = source.edition
        clone.publisherName = source.publisherName
        clone.fullTitle = source.fullTitle

        clone
    }

    static Publication clonePublication(Publication source) {
        Publication clone = new Publication()
        clone.uuid = source.uuid
        clone.publicationDate = source.publicationDate
        clone.title = source.title
        clone.doi = source.doi
        clone.userId = source.userId
        clone.authors = source.authors

        clone
    }

    static Attribute cloneAttribute(Attribute source) {
        Attribute clone = new Attribute()

        clone.uuid = source.uuid
        clone.text = source.text
        clone.id = source.id
        clone.source = source.source

        // title, original, creators & editors are not cloned - copy by reference, not value
        clone.title = source.title
        clone.original = source.original
        clone.creators = source.creators?.collect()
        clone.editors = source.editors?.collect()

        clone
    }
}
