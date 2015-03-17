package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper

class ProfileController extends BaseController {

    def profileService
    def importService

    def saveBHLLinks() {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        if (!json) {
            badRequest()
        } else {
            List<String> linkIds = profileService.saveBHLLinks(json.profileId, json)

            render linkIds as JSON
        }
    }

    def saveLinks() {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        if (!json) {
            badRequest()
        } else {
            List<String> linkIds = profileService.saveLinks(json.profileId, json)

            render linkIds as JSON
        }
    }

    /**
     * Basic search
     * TODO replace with a free text search index backed search.
     * http://grails.github.io/grails-data-mapping/mongodb/manual/guide/3.%20Mapping%20Domain%20Classes%20to%20MongoDB%20Collections.html#3.7%20Full%20Text%20Search
     *
     * @return
     */
    def search() {

        response.setContentType("application/json")
        def opus = Opus.findByUuid(params.opusId)
        if (opus) {
            def results = Profile.findAllByScientificNameIlikeAndOpus(params.scientificName + "%", opus, [max: 10])
            def toRender = []
            results.each { tp ->
                toRender << [
                        "profileId"     : "${tp.uuid}",
                        "guid"          : "${tp.guid}",
                        "scientificName": "${tp.scientificName}"
                ]
            }
            response.setContentType("application/json")
            if (params.callback) {
                render "${params.callback}(${toRender as JSON})"
            } else {
                render toRender as JSON
            }

        } else {
            response.sendError(400)
        }
    }

    def index() {
        log.debug("ProfileController.index")
        def results = Profile.findAll([max: 100], {})
        render(contentType: "application/json") {
            if (results) {
                array {
                    results.each { tp ->
                        taxon(
                                "profileId": "${tp.uuid}",
                                "guid": "${tp.guid}",
                                "scientificName": "${tp.scientificName}"
                        )
                    }
                }
            } else {
                []
            }
        }
    }

    def classification() {
        if (!params.guid || !params.opusId) {
            badRequest "GUID and OpusId are required parameters"
        } else {
            log.debug("Retrieving classification for ${params.guid} in opus ${params.opusId}")
            JsonSlurper js = new JsonSlurper()
            def classification = js.parseText(new URL("${grailsApplication.config.bie.base.url}/ws/classification/${params.guid}").text)

            Opus opus = Opus.findByUuid(params.opusId)

            if (!opus) {
                notFound "No matching Opus was found"
            } else {
                classification.each {
                    def profile = Profile.findByGuidAndOpus(it.guid, opus)
                    it.profileUuid = profile?.uuid ?: ''
                }
            }

            response.setContentType("application/json")
            render classification as JSON
        }
    }

    def getByUuid() {
        // TODO do this better
        log.debug("Fetching profile by profileId ${params.profileId}")
        Profile tp = Profile.findByUuidOrGuidOrScientificName(params.profileId, params.profileId, params.profileId)

        if (tp) {
            respond tp, [formats: ['json', 'xml']]
        } else {
            notFound()
        }
    }

    def importFOA() {
        importService.importFOA()
        render "done"
    }

    def createTestOccurrenceSource() {
        def opus = Opus.findByUuid(profileService.spongesOpusId)

        def testResources = [
                new OccurrenceResource(
                        name: "Test resource 1",
                        webserviceUrl: "http://sandbox.ala.org.au/biocache-service",
                        uiUrl: "http://sandbox.ala.org.au/ala-hub",
                        dataResourceUid: "drt123",
                        pointColour: "CCFF00"
                ),
                new OccurrenceResource(
                        name: "Test resource 2",
                        webserviceUrl: "http://sandbox.ala.org.au/biocache-service",
                        uiUrl: "http://sandbox.ala.org.au/ala-hub",
                        dataResourceUid: "drt125",
                        pointColour: "FFCC00"
                )
        ]

        opus.additionalOccurrenceResources = testResources
        opus.save(flush: true)
    }

    def importFloraBase() {}

    def importSponges() {
        importService.importSponges()
        render "done"
    }
}
