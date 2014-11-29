package au.org.ala.profile

class Link {

    String uuid
    String url
    String title
    String description

    def beforeValidate() {
        if(uuid == null){
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static constraints = {}
}
