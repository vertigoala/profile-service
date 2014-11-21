package au.org.ala.profile

import grails.converters.JSON

class VocabController {

    def index() {
        def vocabs = Vocab.findAll()
        def vocabsToRender = []
        vocabs.each { vocab ->
            vocabsToRender << [
                "uuid":"${vocab.uuid}",
                "name":"${vocab.name}"
            ]
        }
        render vocabsToRender as JSON
    }

    def show(){
        def vocab = Vocab.findByUuid(params.uuid)
        if(vocab){
            def termsToRender = []
            vocab.terms.each { term ->
                termsToRender << [
                        "uuid":"${term.uuid}",
                        "name":"${term.name}"
                ]
            }


            render termsToRender as JSON
        } else {
            response.sendError(404)
        }
    }
}
