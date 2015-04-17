package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Link {

    String uuid
    String url
    String title
    String description
    String doi
    String edition
    String publisherName
    String fullTitle
    String userId

    static hasMany = [creators: Contributor]

    static belongsTo = Profile

    def beforeValidate() {
        if (!uuid) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        doi nullable: true
        edition nullable: true
        publisherName nullable: true
        fullTitle nullable: true
        userId nullable: true
    }
}
