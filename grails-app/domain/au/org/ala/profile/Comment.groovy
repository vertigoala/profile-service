package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Comment {

    String uuid
    String text
    String profileUuid
    Contributor author
    Comment parent
    Date dateCreated

    static hasMany = [children: Comment]

    static constraints = {
        parent nullable: true
    }

    static mapping = {
        children cascade: "all-delete-orphan"
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
