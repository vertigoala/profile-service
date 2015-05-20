package au.org.ala.profile

import grails.converters.JSON

class ImportController extends BaseController {

    static allowedMethods = [vocab: "POST", profile: "POST", glossary: "POST"]

    VocabService vocabService
    ImportService importService
    OpusService opusService

    def vocab() {
        def json = request.getJSON()
        if (!json || !json.opusId || !json.terms) {
            badRequest(samples.vocab)
        } else {
            Opus opus = Opus.findByUuid(json.opusId)

            vocabService.updateVocab(opus.attributeVocabUuid, json)
        }
    }

    def glossary() {
        def json = request.getJSON()
        if (!json || (!json.glossaryId && !json.opusId)) {
            badRequest(samples.glossary)
        } else {
            opusService.saveGlossaryItems(json)
        }
    }

    def profile() {
        def json = request.getJSON()
        if (!json || !json.opusId || !json.profiles) {
            badRequest(samples.profile)
        } else {
            Opus opus = Opus.findByUuid(json.opusId)
            if (!opus) {
                notFound "Opus ${json.opusid} does not exist."
            } else {
                Map<String, String> results = importService.importProfiles(json.opusId, json.profiles)

                render results as JSON
            }
        }
    }


    Map samples = [
            vocab: """{"opusId": "", "strict": "true|false", "deleteExisting": "true|false", "terms": ["term1", "term2", "..."]}""",
            glossary: """{"opusId|glossaryId": "", "items": [{"itemId": "", "term": "", "definition": "", "cf": ["term1","term2", "..."]}]}""",
            profile: """{
                    "opusId": "",
                    "profiles":[{
                            "scientificName": "",
                            "nameAuthor": "",
                            "links":[{
                                    "creators": [""],
                                    "edition": "",
                                    "title": "",
                                    "publisherName": "",
                                    "fullTitle":"",
                                    "description": "",
                                    "url":"",
                                    "doi":""
                                    }],
                            "bhl":[{
                                    "creators": [""],
                                    "edition": "",
                                    "title": "",
                                    "publisherName": "",
                                    "fullTitle":"",
                                    "description": "",
                                    "url":"",
                                    "doi":""
                                    }],
                            "attributes": [{
                                    "creators": [""],
                                    "editors": [""],
                                    "title": "",
                                    "text": ""
                                    }]
                        }]
                     }"""
    ]
}
