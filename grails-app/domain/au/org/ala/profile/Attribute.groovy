package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Attribute implements Comparable<Attribute> {

    static auditable = true

    static searchable = {
        root = false
        only = ["text", "title"]
        title component: true, index: "not_analyzed"
    }

    String uuid
    Term title
    String text // = "This animal lives...."
    Attribute original // The original attribute this was copied from
    String source

    Date dateCreated
    Date lastUpdated

    static hasMany = [creators: Contributor, editors: Contributor]

    static belongsTo = [profile: Profile]

    static constraints = {
        original nullable: true
        source nullable: true
    }

    static mapping = {
    }

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    int compareTo(Attribute right) {
        if (title.order == right.title.order) {
            if (title.name.equalsIgnoreCase(right.title.name)) {
                text <=> right.text
            } else {
                title.name.toLowerCase() <=> right.title.name.toLowerCase()
            }
        } else {
            title.order <=> right.title.order
        }
    }
}
