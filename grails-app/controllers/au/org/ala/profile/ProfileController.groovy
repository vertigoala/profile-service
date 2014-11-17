package au.org.ala.profile

import grails.converters.JSON

class ProfileController {

    def profileService

    def index() {

        def results = Profile.findAll([max:100], {})
//        if(results) {
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
//        }
    }
//
//    def getByScientificName(){
//        def tp = Profile.findByScientificName(params.scientificName)
//        render(contentType: "application/json") {
//            profile (
//                "uuid" : "${tp.uuid}",
//                "guid" : "${tp.guid}",
//                "scientificName" : "${tp.scientificName}",
//                "attributes": array {
//                    tp.attributes.each { attr ->
//                        attribute(
//                            "title":"${attr.title}",
//                            "text":"${attr.text}",
//                            "contributors": array {
//                                attr.contributors.each { contr ->
//                                    contributor("name": contr.name)
//                                }
//                            }
//                        )
//                    }
//                }
//            )
//        }
//    }

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
//           render(contentType: "application/json") {
//               def links = tp.links
//
//               profile (
//                   "uuid" : "${tp.uuid}",
//                   "guid" : "${tp.guid}",
//                   "dataResourceUid" : "${tp.opus.dataResourceUid}",
//                   "opusId" : "${tp.opus.uuid}",
//                   "opusName" : "${tp.opus.title}",
//                   "scientificName" : "${tp.scientificName}",
//                   "links": array {
//                       links.each { lnk ->
//                           link(
//                               "url":"${lnk.url}",
//                               "title":"${lnk.title}",
//                               "description":"${lnk.description}"
//                           )
//                       }
//                   },
//                   "attributes": array {
//                       tp.attributes.each { attr ->
//                           attribute(
//                               "title":"${attr.title}",
//                               "text":"${attr.text}",
//                               "contributor": array {
//                                   attr.contributors.each { c ->
//                                       link(
//                                           "name":"${c.name}",
//                                       )
//                                   }
//                               }
//                           )
//                       }
//                   }
//               )
//           }

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
}
