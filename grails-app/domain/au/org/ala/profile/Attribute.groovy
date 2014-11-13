package au.org.ala.profile

class Attribute {

    String uuid
    String title
    String text // = "This animal lives...."

    Date dateCreated
    Date lastUpdated

    static hasMany = [subAttributes: Attribute, contributors: Contributor]

    static constraints = {}
}
