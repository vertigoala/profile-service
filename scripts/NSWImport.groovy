@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab('org.apache.commons:commons-lang3:3.3.2')

import groovyx.net.http.RESTClient
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

import static groovyx.net.http.ContentType.*

class NSWImport {
    static final String NSW_FLORA_IMAGE_URL_PREFIX = "http://plantnet.rbgsyd.nsw.gov.au/HerbLink/multimedia/"


    static void main(args) {
        def cli = new CliBuilder(usage: "groovy NSWImport -f <datafile> -o opusId -i <imagefile> -p <profileServiceBaseUrl> -d <delimiter default ~> -v <verbose>")
        cli.f(longOpt: "file", "source data file", required: true, args: 1)
        cli.o(longOpt: "opusId", "UUID of the NSW Flora Opus", required: true, args: 1)
        cli.p(longOpt: "profileServiceBaseUrl", "Base URL of the profile service", required: true, args: 1)
        cli.i(longOpt: "imageFile", "File to write image details to", required: true, args: 1)
        cli.d(longOpt: "delimiter", "Data file delimiter (defaults to ~)", required: false, args: 1)
        cli.v(longOpt: "verbose", "Verbose output", required: false, args: 0)

        OptionAccessor opt = cli.parse(args)

        if (!opt) {
            cli.usage()
            return
        }

        String NSW_OPUS_ID = opt.o
        String DATA_FILE = opt.f
        String IMAGE_FILE = opt.i
        String PROFILE_SERVICE_IMPORT_URL = "${opt.p}/import/profile"
        String DELIMITER = opt.d ?: "~"

        List profiles = []

        println "Processing file..."
        int count = 0

        File imageFile = new File(IMAGE_FILE)
        if (imageFile.exists()) {
            imageFile.delete()
            imageFile.createNewFile()
        }

        new File(DATA_FILE).eachLine { line ->
            if (count++ % 50 == 0) println "Processing line ${count}..."

            def fields = clean(line).split(DELIMITER)

            List attributes = []

            String family = fields[1]
            String species = fields[2..4].join(" ").trim()
            String contributor = fields[6]
            List<String> images = fields[24..33].findAll { it }

            if (images) {
                images.each {
                    imageFile << "${species},${NSW_FLORA_IMAGE_URL_PREFIX}${it}\n"
                }
            }

            String suppliedTaxonomy = fields[1..4].join("\n").trim()
            attributes << [title: "Supplied Taxonomy", text: suppliedTaxonomy, creators: [contributor]]

            String commonName = fields[5]
            if (commonName) {
                attributes << [title: "Common Name", text: commonName, creators: [contributor]]
            }

            String description = fields[7]
            if (description) {
                attributes << [title: "Description", text: description, creators: [contributor]]
            }

            String leaves = fields[8]
            if (leaves) {
                attributes << [title: "Leaves", text: leaves, creators: [contributor]]
            }

            String flowers = fields[9]
            if (flowers) {
                attributes << [title: "Flowers", text: flowers, creators: [contributor]]
            }

            String fruit = fields[10]
            if (fruit) {
                attributes << [title: "Fruit", text: fruit, creators: [contributor]]
            }

            String flowering = fields[11]
            if (flowering) {
                attributes << [title: "Flowering", text: flowering, creators: [contributor]]
            }

            String occurrence = fields[12..15].join("\n").trim()
            if (occurrence) {
                attributes << [title: "Occurrence", text: occurrence, creators: [contributor]]
            }

            String otherNotes = fields[18..21].join("\n").trim()
            if (otherNotes) {
                attributes << [title: "Notes", text: otherNotes, creators: [contributor]]
            }

            String synonyms = fields[17]
            if (synonyms) {
                attributes << [title: "Synonyms", text: synonyms, creators: [contributor]]
            }

            String taxonConcept = fields[23]
            if (taxonConcept) {
                attributes << [title: "Taxon Concept", text: taxonConcept, creators: [contributor]]
            }

            Map profile = [scientificName: species ?: family, attributes: attributes]

            profiles << profile
        }

        Map opus = [
                opusId  : NSW_OPUS_ID,
                profiles: profiles
        ]

        println "Importing..."
        def service = new RESTClient(PROFILE_SERVICE_IMPORT_URL)
        def resp = service.post(body: opus, requestContentType: JSON)

        if (resp.status == 200) {
            int success = 0
            resp.data.each { k, v ->
                if (v == "Success") {
                    success++
                } else {
                    println "\t${k} Failed: ${v}"
                }
            }

            println "Imported ${success} of ${count} profiles ${resp.status == 200 ? 'completed successfully' : ' failed'}"
        } else {
            println "Import failed with HTTP ${resp.status}"
        }
    }

    static String clean(String str) {
        unescapeHtml4(str)
    }
}