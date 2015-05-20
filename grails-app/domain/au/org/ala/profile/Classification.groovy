package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Classification {
    String rank
    String guid
    String name

    static constraints = {

    }
}
