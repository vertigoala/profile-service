package au.org.ala.profile

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

    def getByUuid(){
       def tp = Profile.findByUuidOrGuidOrScientificName(params.uuid, params.uuid, params.uuid)
       if(tp){
           render(contentType: "application/json") {
               profile (
                   "uuid" : "${tp.uuid}",
                   "guid" : "${tp.guid}",
                   "dataResourceUid" : "${tp.opus.dataResourceUid}",
                   "opusId" : "${tp.opus.uuid}",
                   "opusName" : "${tp.opus.title}",
                   "scientificName" : "${tp.scientificName}",
                   "attributes": array {
                       tp.attributes.each { attr ->
                           attribute(
                                   "title":"${attr.title}",
                                   "text":"${attr.text}"
                           )
                       }
                   }
               )
           }
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
