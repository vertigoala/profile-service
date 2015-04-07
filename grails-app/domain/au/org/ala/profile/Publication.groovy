package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Publication {
    String uuid
    Date uploadDate
    Date publicationDate
    String title
    String description
    String doi
    String userId
    String authors

    static belongsTo = [profile: Profile]

    def beforeValidate() {
        if (!uuid) {
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        doi nullable: true
    }
}
