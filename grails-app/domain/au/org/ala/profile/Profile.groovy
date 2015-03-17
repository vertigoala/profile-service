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
    }

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }
}
