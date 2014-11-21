package au.org.ala.profile

class Vocab {

    String name

    static hasMany = [ terms:Term ]

    static constraints = {}
}
