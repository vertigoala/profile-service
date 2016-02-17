package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.persistence.Transient

@ToString
@EqualsAndHashCode
class Classification {
    String rank
    String guid
    String name

    // the following transient fields are populated whenever a profile is retrieved, and are used for hierarchical
    // navigation based on the profile's taxonomy
    @Transient
    Integer childCount
    @Transient
    String profileId
    @Transient
    String profileName

    static constraints = {

    }

    static mapping = {
        name index: true
        rank index: true
    }
}
