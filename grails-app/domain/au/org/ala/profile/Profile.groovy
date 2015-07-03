package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.bson.types.ObjectId

@EqualsAndHashCode
@ToString
class Profile {

    ObjectId id
    String uuid
    String guid                 //taxon GUID / LSID
    String scientificName
    String nameAuthor
    String fullName
    String rank
    String nslNameIdentifier
    boolean privateMode = false

    Name matchedName
    List<String> commonNames

    String primaryImage
    List<String> excludedImages
    List<String> specimenIds
    List<Authorship> authorship
    List<Classification> classification
    List<Link> links
    List<Link> bhlLinks
    List<Bibliography> bibliography
    List<Publication> publications

    Date dateCreated
    String createdBy
    Date lastUpdated
    String lastUpdatedBy

    DraftProfile draft

    static embedded = ['authorship', 'classification', 'draft', 'links', 'bhlLinks', 'publications', 'bibliography', 'matchedName']

    static hasMany = [attributes: Attribute]

    static belongsTo = [opus: Opus]

    static constraints = {
        nameAuthor nullable: true
        fullName nullable: true
        guid nullable: true
        primaryImage nullable: true
        excludedImages nullable: true
        specimenIds nullable: true
        classification nullable: true
        nslNameIdentifier nullable: true
        rank nullable: true
        draft nullable: true
        matchedName nullable: true
        createdBy nullable: true
        lastUpdatedBy nullable: true
    }

    static mapping = {
        version false
        attributes cascade: "all-delete-orphan"
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
