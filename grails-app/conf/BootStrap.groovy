import au.org.ala.profile.Term
import au.org.ala.profile.Vocab

class BootStrap {

    def init = { servletContext ->

        def termsToAdd = [
                "CHROMOSOME",
                "COMMENTS",
                "COMMONNAME",
                "CONSERVATION",
                "DESCRIPTION",
                "DISTRIBUTION",
                "ETYMOLOGY",
                "HABITAT",
                "NOTES",
                "PHENOLOGY",
                "PROTOLOGUE",
                "REFERENCE",
                "SOURCE",
                "SPECIMENS",
                "STATUS",
                "TITLE",
                "TYPIFICATION"
        ]

        def vocabName = "Flora of Australia terms"

        def vocab = Vocab.findByName(vocabName)

        if(!vocab){
            vocab = new Vocab(name:vocabName, uuid: "7dba0bab-65d2-4a22-a682-c13b4e301f70")
            vocab.save(flush:true)
        }

        termsToAdd.each {
            def term = Term.findByVocabAndName(vocab, it.toLowerCase().capitalize())
            if(!term){
               term = new Term(name:it.toLowerCase().capitalize(), uuid: UUID.randomUUID().toString(), vocab:vocab)
               term.save(flush:true)
            }
        }
    }
    def destroy = {
    }
}
