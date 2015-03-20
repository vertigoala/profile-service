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

    Date dateCreated
    Date lastUpdated

    static hasMany = [links: Link, attributes: Attribute, bhlLinks: Link]

    static belongsTo = [opus: Opus]

    static constraints = {
        guid nullable: true
    }

    static mapping = {
        version false
        links cascade: "all-delete-orphan"
        bhlLinks cascade: "all-delete-orphan"
        attributes cascade: "all-delete-orphan"
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
