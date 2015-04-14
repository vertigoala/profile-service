package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Bibliography {

    String uuid
    String text
    int order

    static belongsTo = [Profile]

    def beforeValidate() {
        if (!uuid) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {

    }
}
