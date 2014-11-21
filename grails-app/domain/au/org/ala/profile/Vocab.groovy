package au.org.ala.profile

class Vocab {

    String uuid
    String name

    static hasMany = [ terms:Term ]

    static constraints = {}
}
