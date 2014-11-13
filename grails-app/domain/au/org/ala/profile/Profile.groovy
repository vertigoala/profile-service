package au.org.ala.profile

import org.bson.types.ObjectId

class Profile {

    ObjectId id
    String uuid
    String guid                 //taxon GUID / LSID
    String scientificName

    Date dateCreated
    Date lastUpdated

    static hasMany = [attributes: Attribute, links: Link]

    static belongsTo = [opus: Opus]

    static constraints = {
        guid nullable: true
    }

    static mapping = {
        version false
    }
}
