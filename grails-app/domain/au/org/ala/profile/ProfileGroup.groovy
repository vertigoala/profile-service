package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.bson.types.ObjectId

import javax.persistence.Transient

@EqualsAndHashCode
@ToString
/**
 * Represents a group of Profiles, i.e. Season
 */
class ProfileGroup {

    static searchable = {
        only = ["uuid", "language", "englishName"]
        language index: "not_analyzed"
        uuid index: "not_analyzed"
        englishName index: "not_analyzed"
    }

    ObjectId id
    String uuid
    String language // or local name
    String englishName
    String englishMonths
//    String weatherIcon
    String description

    @Transient
    int profileCount

    static transients = ['profileCount']
    static belongsTo = [opus: Opus]

    static constraints = {
        language nullable:  false
        englishName nullable: true
        description nullable: true
        englishMonths nullable: true
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
