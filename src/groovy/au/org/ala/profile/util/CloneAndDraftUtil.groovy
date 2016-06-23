package au.org.ala.profile.util

import au.org.ala.profile.Attachment
import au.org.ala.profile.Attribute
import au.org.ala.profile.Authorship
import au.org.ala.profile.Bibliography
import au.org.ala.profile.Classification
import au.org.ala.profile.Document
import au.org.ala.profile.DraftProfile
import au.org.ala.profile.Link
import au.org.ala.profile.LocalImage
import au.org.ala.profile.Name
import au.org.ala.profile.Profile
import au.org.ala.profile.Publication

class CloneAndDraftUtil {

    static void updateProfileFromDraft(Profile profile) {
        if (!profile.draft) {
            return
        }

        profile.uuid = profile.draft.uuid
        profile.scientificName = profile.draft.scientificName
        profile.nameAuthor = profile.draft.nameAuthor
        profile.fullName = profile.draft.fullName
        profile.taxonomyTree = profile.draft.taxonomyTree
        profile.matchedName = cloneName(profile.draft.matchedName)
        profile.manuallyMatchedName = profile.draft.manuallyMatchedName
        profile.rank = profile.draft.rank
        profile.nslNameIdentifier = profile.draft.nslNameIdentifier
        profile.nslNomenclatureIdentifier = profile.draft.nslNomenclatureIdentifier
        profile.primaryImage = profile.draft.primaryImage
        profile.occurrenceQuery = profile.draft.occurrenceQuery
        profile.imageSettings = profile.draft.imageSettings
        profile.specimenIds = profile.draft.specimenIds
        profile.authorship = profile.draft.authorship
        profile.classification = profile.draft.classification
        profile.links = profile.draft.links
        profile.bhlLinks = profile.draft.bhlLinks
        profile.bibliography = profile.draft.bibliography
        profile.publications = profile.draft.publications
        profile.privateImages = profile.draft.privateImages
        profile.attachments = profile.draft.attachments
        profile.manualClassification = profile.draft.manualClassification

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
        clone.manuallyMatchedName = profile.manuallyMatchedName
        clone.rank = profile.rank
        clone.guid = profile.guid
        clone.taxonomyTree = profile.taxonomyTree
        clone.nslNameIdentifier = profile.nslNameIdentifier
        clone.nslNomenclatureIdentifier = profile.nslNomenclatureIdentifier
        clone.primaryImage = profile.primaryImage
        clone.occurrenceQuery = profile.occurrenceQuery
        clone.imageSettings = profile.imageSettings?.clone()
        clone.specimenIds = profile.specimenIds?.collect()
        clone.authorship = profile.authorship?.collect { cloneAuthorship(it) }
        clone.classification = profile.classification?.collect { cloneClassification(it) }
        clone.manualClassification = profile.manualClassification
        clone.links = profile.links?.collect { cloneLink(it) }
        clone.bhlLinks = profile.bhlLinks?.collect { cloneLink(it) }
        clone.bibliography = profile.bibliography?.collect { cloneBibliography(it) }
        clone.documents = profile.documents?.collect { cloneDocuments(it) }
        clone.publications = profile.publications?.collect { clonePublication(it) }
        clone.attributes = profile.attributes?.collect { cloneAttribute(it) }
        clone.privateImages = profile.privateImages?.collect { cloneImage(it) }
        clone.attachments = profile.attachments?.collect { cloneAttachment(it) }

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

    static Bibliography cloneBibliography(Bibliography source, boolean includeIds = true) {
        Bibliography clone = new Bibliography()
        if (includeIds) {
            clone.uuid = source.uuid
        }
        clone.text = source.text
        clone.order = source.order

        clone
    }

    static Document cloneDocuments(Document source, boolean includeIds = true) {
        Document clone = new Document()
        if (includeIds) {
            clone.documentId = source.documentId
        }
        clone.attribution = source.attribution
        clone.embeddedAudio = source.embeddedAudio
        clone.embeddedVideo = source.embeddedVideo
        clone.isPrimaryProjectImage = source.isPrimaryProjectImage
        clone.labels = source.labels
        clone.licence = source.licence
        clone.name = source.name
        clone.parentId = source.parentId
        clone.primaryAudio = source.primaryAudio
        clone.primaryVideo = source.primaryVideo
        clone.role = source.role
        clone.status = source.status
        clone.thirdPartyConsentDeclarationMade = source.thirdPartyConsentDeclarationMade
        clone.thirdPartyConsentDeclarationText = source.thirdPartyConsentDeclarationText

        clone
    }

    static Link cloneLink(Link source, boolean includeIds = true) {
        Link clone = new Link()
        if (includeIds) {
            clone.uuid = source.uuid
        }
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

    static Attribute cloneAttribute(Attribute source, boolean includeIds = true) {
        Attribute clone = new Attribute()

        if (includeIds) {
            clone.uuid = source.uuid
            clone.id = source.id
        }
        clone.text = source.text
        clone.source = source.source

        // title, original, creators & editors are not cloned - copy by reference, not value
        clone.title = source.title
        clone.original = source.original
        clone.creators = source.creators?.collect()
        clone.editors = source.editors?.collect()

        clone
    }

    static LocalImage cloneImage(LocalImage source) {
        LocalImage clone = new LocalImage()

        clone.creator = source.creator
        clone.created = source.created
        clone.description = source.description
        clone.imageId = source.imageId
        clone.licence = source.licence
        clone.originalFileName = source.originalFileName
        clone.rights = source.rights
        clone.rightsHolder = source.rightsHolder
        clone.title = source.title
        clone.contentType = source.contentType

        clone
    }

    static Attachment cloneAttachment(Attachment source) {
        Attachment clone = new Attachment()

        clone.creator = source.creator
        clone.createdDate = source.createdDate
        clone.description = source.description
        clone.contentType = source.contentType
        clone.licence = source.licence
        clone.filename = source.filename
        clone.rights = source.rights
        clone.rightsHolder = source.rightsHolder
        clone.title = source.title
        clone.uuid = source.uuid
        clone.url = source.url

        clone
    }
}
