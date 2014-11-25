package au.org.ala.profile

class OpusController {

    def index() {

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
                    "bannerUrl": result.bannerUrl,
                    "attributeVocabUuid": result.attributeVocabUuid,
                    "enablePhyloUpload":result.enablePhyloUpload,
                    "enableOccurrenceUpload":result.enableOccurrenceUpload,
                    "enableTaxaUpload":result.enableTaxaUpload,
                    "enableKeyUpload":result.enableKeyUpload
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
                    "bannerUrl": result.bannerUrl,
                    "attributeVocabUuid": result.attributeVocabUuid,
                    "enablePhyloUpload":result.enablePhyloUpload,
                    "enableOccurrenceUpload":result.enableOccurrenceUpload,
                    "enableTaxaUpload":result.enableTaxaUpload,
                    "enableKeyUpload":result.enableKeyUpload
                ]
            }
        } else {
            response.sendError(404)
        }
    }
}
