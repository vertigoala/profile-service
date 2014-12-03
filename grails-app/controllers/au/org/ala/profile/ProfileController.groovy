package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper

class ProfileController {

    def profileService

    def saveBHLLinks(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def profile = Profile.findByUuid(json.profileUuid)
        def uuids = []
        def linksToSave = []
        if(profile){
            if(json.links){
                json.links.each {
                    def link
                    if(it.uuid){
                        link = Link.findByUuid(it.uuid)
                    } else {
                        link = new Link(uuid:UUID.randomUUID().toString())
                    }

                    link.url = it.url
                    link.title = it.title
                    link.description = it.description
                    link.fullTitle = it.fullTitle
                    link.edition = it.edition
                    link.publisherName = it.publisherName
                    link.doi = it.doi
                    linksToSave << link
                    uuids << link.uuid
                }

                profile.bhlLinks = linksToSave
                profile.save(flush:true)
                profile.errors.allErrors.each {
                    println it
                }
            }
        }
        render uuids as JSON
    }

    def saveLinks(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def profile = Profile.findByUuid(json.profileUuid)
        def uuids = []
        def linksToSave = []
        if(profile){
            if(json.links){
                json.links.each {
                    def link
                    if(it.uuid){
                        link = Link.findByUuid(it.uuid)
                    } else {
                        link = new Link(uuid:UUID.randomUUID().toString())
                    }
                    link.url = it.url
                    link.title = it.title
                    link.description = it.description
                    link.errors.allErrors.each {
                        println it
                    }
                    linksToSave << link
                    uuids << link.uuid
                }
                profile.links = linksToSave
                profile.save(flush:true)
                profile.errors.allErrors.each {
                    println it
                }
            }
        }
        render uuids as JSON
    }

    /**
     * Basic search
     * TODO replace with a free text search index backed search.
     *
     * @return
     */
    def search(){

        response.setContentType("application/json")
        def opus = Opus.findByUuid(params.opusUuid)
        if(opus){
            def results = Profile.findAllByScientificNameIlikeAndOpus(params.scientificName+"%", opus, [max:10])
            def toRender = []
            results.each { tp ->
                toRender << [
                    "uuid": "${tp.uuid}",
                    "guid": "${tp.guid}",
                    "scientificName": "${tp.scientificName}"
                ]
            }
            response.setContentType("application/json")
            if(params.callback){
                render "${params.callback}(${toRender as JSON})"
            } else {
                render toRender as JSON
            }

        } else {
            response.sendError(400)
        }
    }

    def index() {
        def results = Profile.findAll([max:100], {})
        render(contentType: "application/json") {
            if(results) {
                array {
                    results.each { tp ->
                        taxon(
                                "uuid": "${tp.uuid}",
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

    def classification(){

        println("Retrieving classification for: " + params.guid)

        if(params.guid){
            def js = new JsonSlurper()
            def classification = js.parseText(new URL("http://bie.ala.org.au/ws/classification/" + params.guid).text)
            classification.each {
                def profile = Profile.findByGuid(it.guid)
                it.profileUuid = profile?.uuid?:''
            }

            response.setContentType("application/json")
            render classification as JSON
        } else {
            response.sendError(400)
        }
    }

    def getByUuid(){
       def tp = Profile.findByUuidOrGuidOrScientificName(params.uuid, params.uuid, params.uuid)

       if(tp){

           def attributesToRender = []
           tp.attributes.each { attr ->
               attributesToRender << [
                   "uuid":"${attr.uuid}",
                   "title":"${attr.title}",
                   "text":"${attr.text}",
                   "creators": attr.creators.collect { it.name },
                   "editors": attr.editors.collect { it.name }
               ]
           }

           attributesToRender.sort { it.title.toLowerCase() }

           def linksToRender = []
           tp.links.each {
               linksToRender << [
                   "uuid":"${it.uuid}",
                   "url":"${it.url}",
                   "title":"${it.title}",
                   "description": "${it.description}",
                   "creators": it.creators.collect { it.name },
               ]
           }

           def bhlToRender = []
           tp.bhlLinks.each {
               bhlToRender << [
                   "uuid":"${it.uuid}",
                   "url":"${it.url}",
                   "title":"${it.title}",
                   "fullTitle":"${it.fullTitle}",
                   "edition":"${it.edition}",
                   "publisherName":"${it.publisherName}",
                   "doi":"${it.doi}",
                   "description": "${it.description}",
                   "creators": it.creators.collect { it.name }
               ]
           }

           def response =  [
               "uuid" : "${tp.uuid}",
               "guid" : "${tp.guid}",
               "dataResourceUid" : "${tp.opus.dataResourceUid}",
               "opusId" : "${tp.opus.uuid}",
               "opusName" : "${tp.opus.title}",
               "scientificName" : "${tp.scientificName}",
               "attributes": attributesToRender,
               "links":linksToRender,
               "bhl":bhlToRender
           ]

           render response as JSON

       } else {
           response.sendError(404, "Identifier unrecognised: " + params.uuid)
       }
    }

    def importFOA(){
        profileService.importFOA()
        render "done"
    }

    def createTestOccurrenceSource(){
        def opus = Opus.findByUuid(profileService.spongesUuid)

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
        opus.save(flush:true)
    }

    def importFloraBase(){}

    def importSponges() {
        profileService.importSponges()
        render "done"
    }
}
