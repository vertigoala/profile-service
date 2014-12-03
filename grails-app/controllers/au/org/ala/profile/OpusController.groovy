package au.org.ala.profile

import au.com.bytecode.opencsv.CSVReader
import grails.converters.JSON

class OpusController {

    def nameService

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
                    "biocacheName":result.biocacheName?:'Atlas',
                    "additionalOccurrenceResources" : result.additionalOccurrenceResources ?: []
                ]
            }
        } else {
            response.sendError(404)
        }
    }

    def taxaUpload(){
        println("taxa upload invoked....")
        def file = request.getFile('taxaUploadFile')
        def opus = Opus.findByUuid(params.opusId)

        if(file) {
            println("files provided")
            def tmpFile = new File("/tmp/taxa-upload.txt")
            file.transferTo(tmpFile)
            def reader = new CSVReader(new FileReader(tmpFile))
            def columnHeaders = (reader.readNext() as List).collect { it.trim().toLowerCase() }
            def currentLine
            def taxaCreated = 0
            def linesSkipped = 0
            def alreadyExists = 0
            if(columnHeaders.contains("scientificname")){
                def columnIdx = columnHeaders.indexOf("scientificname")
                while((currentLine = reader.readNext()) != null){
                    if(currentLine.length > columnIdx) {
                        def scientificName = currentLine[columnIdx]
                        if(scientificName && scientificName.trim()) {
                            def profile = Profile.findByOpusAndScientificName(opus, scientificName)
                            if(profile){
                                alreadyExists++
                            } else {
                                profile = new Profile([
                                        scientificName: scientificName,
                                        guid: nameService.getGuidForName(scientificName) ?: "",
                                        opus: opus
                                ])
                                profile.save(flush: true)
                                taxaCreated++
                            }

                        } else {
                            linesSkipped++
                        }
                    }
                }
            }
            response.setContentType("application/json")
            def model = [taxaCreated:taxaCreated, linesSkipped:linesSkipped, alreadyExists: alreadyExists]
            render model as JSON
        } else {
            println("No file received")
        }
    }
}
