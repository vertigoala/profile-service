package au.org.ala.profile

import groovy.json.JsonSlurper

class OpusController {

    def index() {

        def js = new JsonSlurper()
        def opui = Opus.findAll()
        render(contentType: "application/json") {
            opui: array {
                opui.each { result ->
                    opus(
                    "dataResourceUid": "${result.dataResourceUid}",
                    "uuid": "${result.uuid}",
                    "title": "${result.title}",
                    "imageSources": result.imageSources,
                    "recordSources": result.recordSources
                )
                }
            }
        }
    }

    def show(){
        def result = Opus.findByUuid(params.uuid)
        if(result){
            render(contentType: "application/json") {
                [
                    "dataResourceUid": "${result.dataResourceUid}",
                    "uuid" : "${result.uuid}",
                    "title" : "${result.title}",
                    "imageSources" : result.imageSources,
                    "recordSources" : result.recordSources
                ]
            }
        } else {
            response.sendError(404)
        }
    }
}
