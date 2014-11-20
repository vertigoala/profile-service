package au.org.ala.profile

class Contributor {

    String uuid
    String userId  //CAS ID may be null
    String name
    String dataResourceUid

    Date dateCreated
    Date lastUpdated

    static constraints = {
        userId nullable: true
        dataResourceUid nullable: true
    }

    static mapping = {
        version false
    }
}
