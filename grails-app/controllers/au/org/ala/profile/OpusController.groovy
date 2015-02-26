package au.org.ala.profile

import au.com.bytecode.opencsv.CSVReader
import grails.converters.JSON

class OpusController {

    def nameService

    def index() {
        respond Opus.findAll(), [formats: ['json', 'xml']]
    }

    def show() {
        def result = Opus.findByUuid(params.opusId)
        if (result) {
            respond result, [formats: ['json', 'xml']]
        } else {
            response.sendError(404)
        }
    }

    def taxaUpload() {
        log.info("taxa upload invoked....")
        def file = request.getFile('taxaUploadFile')
        def opus = Opus.findByUuid(params.opusId)

        if (file) {
            log.info("files provided")
            def tmpFile = new File("/tmp/taxa-upload.txt")
            file.transferTo(tmpFile)
            def reader = new CSVReader(new FileReader(tmpFile))
            def columnHeaders = (reader.readNext() as List).collect { it.trim().toLowerCase() }
            def currentLine
            def taxaCreated = 0
            def linesSkipped = 0
            def alreadyExists = 0
            if (columnHeaders.contains("scientificname")) {
                def columnIdx = columnHeaders.indexOf("scientificname")
                while ((currentLine = reader.readNext()) != null) {
                    if (currentLine.length > columnIdx) {
                        def scientificName = currentLine[columnIdx]
                        if (scientificName && scientificName.trim()) {
                            def profile = Profile.findByOpusAndScientificName(opus, scientificName)
                            if (profile) {
                                alreadyExists++
                            } else {
                                profile = new Profile([
                                        scientificName: scientificName,
                                        guid          : nameService.getGuidForName(scientificName) ?: "",
                                        opus          : opus
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
            def model = [taxaCreated: taxaCreated, linesSkipped: linesSkipped, alreadyExists: alreadyExists]
            render model as JSON
        } else {
            log.info("No file received")
        }
    }
}
