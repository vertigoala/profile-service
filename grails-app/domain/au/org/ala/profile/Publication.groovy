package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Publication {
    String uuid
    Date publicationDate
    String title
    Integer version
    String doi
    String userId
    String authors

    def beforeValidate() {
        if (!uuid) {
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        doi nullable: true
    }
}
