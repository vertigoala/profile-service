package au.org.ala.profile

class Attribute {

    static auditable = true

    String uuid
    String title
    String text // = "This animal lives...."

    Date dateCreated
    Date lastUpdated

    static hasMany = [subAttributes: Attribute, creators: Contributor, editors: Contributor]

    // The original attribute this was copied from
    static belongsTo = [original: Attribute]

    static constraints = {
        original nullable:true
    }

    def beforeValidate() {
        if(uuid == null){
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }
}
