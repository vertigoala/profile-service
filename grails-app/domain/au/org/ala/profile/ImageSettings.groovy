package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml
import au.org.ala.profile.util.ImageOption
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import static au.org.ala.profile.sanitizer.SanitizerPolicyConstants.SINGLE_LINE

@EqualsAndHashCode
@ToString
class ImageSettings {

    @SanitizedHtml(SINGLE_LINE)
    String caption
    ImageOption imageDisplayOption

    static mapping = {
        version false
    }

    static constraints = {
        caption nullable: true
        imageDisplayOption nullable: true
    }
}
