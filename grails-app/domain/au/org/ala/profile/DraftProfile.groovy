package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class DraftProfile {

    String uuid
    String guid                 //taxon GUID / LSID
    String scientificName
    String nameAuthor
    String fullName
    Name matchedName
    boolean manuallyMatchedName = false
    String rank
    String nslNameIdentifier
    String nslNomenclatureIdentifier
    String taxonomyTree
    String primaryImage
    String primaryVideo
    String primaryAudio
    boolean showLinkedOpusAttributes = false // Even if set to true, this needs Opus.showLinkedOpusAttributes to also be true
    String occurrenceQuery
    boolean isCustomMapConfig = false
    String profileStatus = Profile.STATUS_PARTIAL
    Map<String, ImageSettings> imageSettings = [:]
    List<String> specimenIds
    List<Authorship> authorship
    List<Classification> classification
    boolean manualClassification = false
    List<Link> links
    List<Link> bhlLinks
    List<Attribute> attributes
    List<Publication> publications
    List<Bibliography> bibliography
    List<Document> documents
    List<LocalImage> stagedImages
    List<LocalImage> privateImages
    List<Attachment> attachments
    String lastAttributeChange

    Date dateCreated
    Date draftDate = new Date()
    String createdBy
    Date lastPublished

    // Omitting imageSettings prevented mongo from storing the embedded Map properly, however the Integration Tests
    // didn't fail because of that.
    static embedded = ['authorship', 'classification', 'draft', 'links', 'bhlLinks', 'publications', 'bibliography', 'documents', 'attributes', 'stagedImages', 'imageSettings', 'privateImages', 'attachments']

    static constraints = {
        nameAuthor nullable: true
        guid nullable: true
        primaryImage nullable: true
        primaryVideo nullable: true
        primaryAudio nullable: true
        nslNameIdentifier nullable: true
        nslNomenclatureIdentifier nullable: true
        rank nullable: true
        fullName nullable: true
        matchedName nullable: true
        taxonomyTree nullable: true
        createdBy nullable: true
        lastAttributeChange nullable: true
        occurrenceQuery nullable: true
        profileStatus nullable: true
    }
}
