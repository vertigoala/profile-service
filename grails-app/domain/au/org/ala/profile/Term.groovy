package au.org.ala.profile

class Term {

    String name

    static belongsTo = [vocab:Vocab]

    static constraints = {}
}
