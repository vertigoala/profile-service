package au.org.ala.profile

import grails.converters.JSON

class ProfileController {

    def profileService

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
        def classification = []
        def availableProfiles = []
        if(profile.guid){
            classification = js.parseText(new URL("http://bie.ala.org.au/ws/classification/" + params.guid).text)
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
                   "contributor": attr.contributors.collect{ it.name }
               ]
           }

           def linksToRender = []
           tp.links.each {
               linksToRender << [

                   "url":"${it.url}",
                   "title":"${it.title}",
                   "description": "${it.description}"
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
               "links":linksToRender
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

    def importFloraBase(){
    }

    def importSponges() {
        profileService.importSponges()
        render "done"
    }
}
