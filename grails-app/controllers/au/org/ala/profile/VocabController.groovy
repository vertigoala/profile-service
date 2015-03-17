package au.org.ala.profile

import grails.converters.JSON

class VocabController extends BaseController {

    VocabService vocabService

    def index() {
        def vocabs = Vocab.findAll()
        def vocabsToRender = []
        vocabs.each { vocab ->
            vocabsToRender << [
                    "name": "${vocab.name}",
                    "vocabId": "${vocab.uuid}",
                    "strict": "${vocab.strict ?: false}"
            ]
        }

        render vocabsToRender.sort { it.name.toLowerCase() } as JSON
    }

    def show() {
        def vocab = Vocab.findByUuid(params.vocabId)
        if (vocab) {
            def termsToRender = []
            vocab.terms.each { term ->
                termsToRender << [
                        "name": "${term.name}",
                        "termId": "${term.uuid}"
                ]
            }

            def payload = [name: vocab.name, strict: vocab.strict ?: false, terms: termsToRender.sort { it.name.toLowerCase() }]
            render payload as JSON
        } else {
            notFound()
        }
    }

    def update() {
        if (!params.vocabId) {
            badRequest()
        } else {
            Vocab vocab = Vocab.findByUuid(params.vocabId);

            if (!vocab) {
                notFound()
            } else {
                def json = request.getJSON()

                boolean updated = vocabService.updateVocab(params.vocabId, json);

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    def findUsagesOfTerm() {
        if (!params.vocabId || !params.term) {
            badRequest()
        } else {
            int usages = vocabService.findUsagesOfTerm(params.vocabId, params.term)

            render ([usageCount: usages] as JSON)
        }
    }

    def replaceUsagesOfTerm() {
        def json = request.getJSON();
        if (!json) {
            badRequest()
        } else {
            Map<String, Integer> usages = vocabService.replaceUsagesOfTerm(json)

            render ([usages: usages] as JSON)
        }
    }

}
