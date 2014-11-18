package au.org.ala.profile

import grails.converters.JSON

class ProfileController {

    def profileService

    def search(){

        def opus = Opus.findByUuid(params.opusUuid)
        if(opus){
            def results = Profile.findAllByScientificNameIlikeAndOpus(params.scientificName+"%", opus)
            render(contentType: "application/json") {
                if(results) {
                    profiles = array {
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
        } else {
            response.sendError(400)
        }
    }

    def index() {
        def results = Profile.findAll([max:100], {})
        render(contentType: "application/json") {
            if(results) {
                profiles = array {
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
            response.sendError(404, "Identifier unrecognised: "+ params.uuid)
       }
    }

    def importFOA(){
        profileService.importFOA()
        render "done"
    }

    def importFloraBase(){
    }

    def importSponges(){


    }
}
