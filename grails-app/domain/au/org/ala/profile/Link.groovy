package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import static au.org.ala.profile.sanitizer.SanitizerPolicyConstants.SINGLE_LINE

@EqualsAndHashCode
@ToString
class Link {

    String uuid
    String url
    @SanitizedHtml(SINGLE_LINE)
    String title
    @SanitizedHtml(SINGLE_LINE)
    String description
    String doi
    String edition
    String publisherName
    String fullTitle

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
    }
}
