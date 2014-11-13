package au.org.ala.profile

class OpusController {

    def index() {}

    def show(){
        def result = Opus.findByUuid(params.uuid)
        if(result){
            render(contentType: "application/json") {
                opus (
                    "uuid" : "${result.uuid}",
                    "title" : "${result.title}",
                    "imageSources" : result.imageSources,
                    "recordSources" : result.recordSources
                )
            }
        } else {
            response.sendError(404)
        }
    }
}
