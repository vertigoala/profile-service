package au.org.ala.profile

import au.org.ala.profile.util.ImageOption
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class ImageSettings {

    String caption
    ImageOption imageDisplayOption

    static mapping = {
        version false
    }
}
