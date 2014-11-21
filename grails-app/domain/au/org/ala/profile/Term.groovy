package au.org.ala.profile

class Term {

    String uuid
    String name

    static belongsTo = [vocab:Vocab]

    static constraints = {}
}
