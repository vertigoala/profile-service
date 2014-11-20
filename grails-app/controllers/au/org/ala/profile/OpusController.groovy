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
                    "uuid": "${result.uuid}",
                    "dataResourceUid": "${result.dataResourceUid}",
                    "title": "${result.title}",
                    "imageSources": result.imageSources,
                    "recordSources": result.recordSources,
                    "logoUrl": result.logoUrl,
                    "bannerUrl": result.bannerUrl
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
                    "uuid" : "${result.uuid}",
                    "dataResourceUid": "${result.dataResourceUid}",
                    "title" : "${result.title}",
                    "imageSources" : result.imageSources,
                    "recordSources" : result.recordSources,
                    "logoUrl": result.logoUrl,
                    "bannerUrl": result.bannerUrl
                ]
            }
        } else {
            response.sendError(404)
        }
    }
}
