package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Vocab {

    String uuid
    String name

    static hasMany = [terms: Term]

    static constraints = {}

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }
}
