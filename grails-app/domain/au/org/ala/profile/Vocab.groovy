package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes = ['uuid','name','strict'])
@ToString(includes = ['uuid','name','strict'])
class Vocab {

    String uuid
    String name
    boolean strict = false

    static hasMany = [terms: Term]

    static constraints = {}

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
