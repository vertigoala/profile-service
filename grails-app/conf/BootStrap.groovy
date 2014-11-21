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
            vocab = new Vocab(name:vocabName)
            vocab.save(flush:true)
        }

        termsToAdd.each {
            def term = Term.findByVocabAndName(vocab, it)
            if(!term){
               term = new Term(name:it.toLowerCase().capitalize(), vocab:vocab)
               term.save(flush:true)
            }
        }
    }
    def destroy = {
    }
}
