package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class GlossaryItem {
    String uuid
    String term
    String description

    static hasMany = [cf: GlossaryItem]

    static belongsTo = [glossary: Glossary]

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }
}
