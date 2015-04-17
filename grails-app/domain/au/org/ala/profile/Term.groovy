package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Term {

    String uuid
    String name

    static belongsTo = [vocab: Vocab]

    static constraints = {}

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
