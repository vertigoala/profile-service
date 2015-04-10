package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Glossary {
    String uuid

    static hasMany = [items: GlossaryItem]

    static constraints = {
    }

    static mapping = {
        version false
        items cascade: "all-delete-orphan"
    }

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
