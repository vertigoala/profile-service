package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.bson.types.ObjectId

import javax.persistence.Transient

@EqualsAndHashCode
@ToString
class Profile {

    private static final String NOT_ANALYZED_INDEX = "not_analyzed"

    static searchable = {
        only = ["uuid", "guid", "scientificName", "fullName", "matchedName", "rank", "primaryImage", "opus", "attributes", "lastUpdated", "archivedDate", "archivedWithName", "scientificNameLower", "archivedNameLower", "matchedNameLower"]
        scientificName multi_field: true, boost: 20
        archivedWithName multi_field: true, boost: 20
        matchedName component: true, boost: 10
        opus component: true
        attributes component: true
        uuid index: NOT_ANALYZED_INDEX
        guid index: NOT_ANALYZED_INDEX
        lastUpdated index: NOT_ANALYZED_INDEX
        rank index: NOT_ANALYZED_INDEX
        primaryImage index: NOT_ANALYZED_INDEX
        scientificNameLower index: NOT_ANALYZED_INDEX
        archivedNameLower index: NOT_ANALYZED_INDEX
        matchedNameLower index: NOT_ANALYZED_INDEX
    }

    ObjectId id
    String uuid
    String guid                 //taxon GUID / LSID
    String scientificName
    String nameAuthor
    String fullName
    String rank
    String nslNameIdentifier
    String nslNomenclatureIdentifier
    String nslProtologue

    @Transient
    boolean privateMode = false

    Name matchedName
    String taxonomyTree
    String primaryImage
    Map<String, ImageSettings> imageSettings = [:]
    boolean showLinkedOpusAttributes = false // Even if set to true, this needs Opus.showLinkedOpusAttributes to also be true
    List<String> specimenIds
    List<Authorship> authorship
    List<Classification> classification
    List<Link> links
    List<Link> bhlLinks
    List<Bibliography> bibliography
    List<Publication> publications

    List<LocalImage> privateImages = []
    List<LocalImage> stagedImages = [] // this is only used when dealing with draft profiles

    List<Attachment> attachments

    String lastAttributeChange

    Date dateCreated
    String createdBy
    Date lastUpdated
    String lastUpdatedBy

    DraftProfile draft

    String archiveComment
    Date archivedDate
    String archivedBy
    String archivedWithName

    @Transient
    String getScientificNameLower() { scientificName?.toLowerCase() }
    @Transient
    String getArchivedNameLower() { archivedWithName?.toLowerCase() }
    @Transient
    String getMatchedNameLower() { matchedName?.scientificName?.toLowerCase() }

    static embedded = ['authorship', 'classification', 'draft', 'links', 'bhlLinks', 'publications', 'bibliography', 'matchedName', 'privateImages', 'attachments', 'imageSettings']

    static hasMany = [attributes: Attribute]

    static belongsTo = [opus: Opus]

    static constraints = {
        nameAuthor nullable: true
        fullName nullable: true
        guid nullable: true
        primaryImage nullable: true
        specimenIds nullable: true
        classification nullable: true
        nslNameIdentifier nullable: true
        nslNomenclatureIdentifier nullable: true
        nslProtologue nullable: true
        rank nullable: true
        draft nullable: true
        taxonomyTree nullable: true
        matchedName nullable: true
        createdBy nullable: true
        lastUpdatedBy nullable: true
        lastAttributeChange nullable: true
        archiveComment nullable: true
        archivedDate nullable: true
        archivedBy nullable: true
        archivedWithName nullable: true
    }

    static mapping = {
        attributes cascade: "all-delete-orphan"
        scientificName index: true
        guid index: true
        rank index: true
        uuid index: true
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
