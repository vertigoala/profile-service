package au.org.ala.profile

class Link {

    String uuid
    String url
    String title
    String description
    String doi
    String edition
    String publisherName
    String fullTitle
    String userId

    static hasMany = [creators: Contributor]

    def beforeValidate() {
        if(!uuid){
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        doi nullable: true
        edition nullable: true
        publisherName nullable: true
        fullTitle nullable: true
        userId nullable: true
    }
}
