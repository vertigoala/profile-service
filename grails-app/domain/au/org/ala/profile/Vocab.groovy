package au.org.ala.profile

class Vocab {

    String uuid
    String name

    static hasMany = [ terms:Term ]

    static constraints = {}

    def beforeValidate() {
        if(uuid == null){
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }
}
