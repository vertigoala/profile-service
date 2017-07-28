package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml
//import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString(includes = ['uuid', 'text', 'profileUuid', 'author'])
//@EqualsAndHashCode
class Comment {

    String uuid
    @SanitizedHtml
    String text
    String profileUuid
    Contributor author
    Comment parent
    Date dateCreated
    Date lastUpdated

//    static belongsTo = [parent: Comment]
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

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof Comment)) return false

        Comment comment = (Comment) o

        if (profileUuid != comment.profileUuid) return false
        if (text != comment.text) return false
        if (uuid != comment.uuid) return false

        return true
    }

    int hashCode() {
        int result
        result = (uuid != null ? uuid.hashCode() : 0)
        result = 31 * result + (text != null ? text.hashCode() : 0)
        result = 31 * result + (profileUuid != null ? profileUuid.hashCode() : 0)
        return result
    }
}
