package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Term implements Comparable<Term> {

    String uuid
    String name
    int order = -1

    static belongsTo = [vocab: Vocab]

    static constraints = {}

    def beforeValidate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
        }
    }

    @Override
    int compareTo(Term other) {
        if (order == other.order) {
            name.toLowerCase() <=> other.name.toLowerCase()
        } else {
            order <=> other.order
        }
    }
}
