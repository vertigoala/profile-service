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
                    "enablePhyloUpload":result.enablePhyloUpload != null ? result.enableKeyUpload : true,
                    "enableOccurrenceUpload":result.enableOccurrenceUpload != null ? result.enableKeyUpload : true,
                    "enableTaxaUpload":result.enableTaxaUpload != null ? result.enableKeyUpload : true,
                    "enableKeyUpload":result.enableKeyUpload != null ? result.enableKeyUpload : true,
                    "mapAttribution":result.mapAttribution?:'Atlas',
                    "biocacheUrl":result.biocacheUrl?:'http://biocache.ala.org.au',
                    "biocacheName":result.biocacheName?:'Atlas'
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
                    "enablePhyloUpload":result.enablePhyloUpload  != null ? result.enableKeyUpload : true,
                    "enableOccurrenceUpload":result.enableOccurrenceUpload != null ? result.enableKeyUpload : true,
                    "enableTaxaUpload":result.enableTaxaUpload != null ? result.enableKeyUpload : true,
                    "enableKeyUpload":result.enableKeyUpload != null ? result.enableKeyUpload : true,
                    "mapAttribution":result.mapAttribution?:'Atlas',
                    "biocacheUrl":result.biocacheUrl?:'http://biocache.ala.org.au',
                    "biocacheName":result.biocacheName?:'Atlas'
                ]
            }
        } else {
            response.sendError(404)
        }
    }
}
