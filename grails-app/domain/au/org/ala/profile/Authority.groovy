package au.org.ala.profile

import au.org.ala.profile.security.Role
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Authority {

    Contributor user
    Role role
    String notes

    static belongsTo = [Opus, Contributor]

    static constraints = {
        notes nullable: true
    }
}
