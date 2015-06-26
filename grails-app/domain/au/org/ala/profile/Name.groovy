package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Name {

    String scientificName
    String nameAuthor
    String fullName
    String guid

    static constraints = {
        scientificName nullable: true
        nameAuthor nullable: true
        fullName nullable: true
        guid nullable: true
    }
}
