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

    String primaryImage
    List<String> excludedImages
    List<String> specimenIds
    List<Authorship> authorship

    Date dateCreated
    Date lastUpdated

    static embedded = ['authorship']

    static hasMany = [links: Link, attributes: Attribute, bhlLinks: Link, publications: Publication, bibliography: Bibliography]

    static belongsTo = [opus: Opus]

    static constraints = {
        guid nullable: true
        primaryImage nullable: true
        excludedImages nullable: true
        specimenIds nullable: true
    }

    static mapping = {
        version false
        links cascade: "all-delete-orphan"
        bhlLinks cascade: "all-delete-orphan"
        attributes cascade: "all-delete-orphan"
        publications cascade: "all-delete-orphan"
        bibliography cascade: "all-delete-orphan"
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
