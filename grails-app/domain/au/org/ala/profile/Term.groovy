package au.org.ala.profile

class Term {

    String uuid
    String name

    static belongsTo = [vocab: Vocab]

    static constraints = {}

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }
}
