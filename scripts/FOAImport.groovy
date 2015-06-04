@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab('com.xlson.groovycsv:groovycsv:1.0')
@Grab('org.apache.commons:commons-lang3:3.3.2')

import groovyx.net.http.RESTClient

import static groovyx.net.http.ContentType.*
import static com.xlson.groovycsv.CsvParser.parseCsv

class NSWImport {

    static String OPUS_ID
    static String DATA_DIR
    static String IMAGE_FILE
    static String REPORT_FILE
    static String PROFILE_SERVICE_IMPORT_URL
    static String DELIMITER


    static void main(args) {
        def cli = new CliBuilder(usage: "groovy FOAImport -f <datadir> -o opusId -i <imagefile> -p <profileServiceBaseUrl> -d <delimiter default ~> -r <reportfile>")
        cli.f(longOpt: "dir", "source data directory", required: true, args: 1)
        cli.o(longOpt: "opusId", "UUID of the FOA Opus", required: true, args: 1)
        cli.p(longOpt: "profileServiceBaseUrl", "Base URL of the profile service", required: true, args: 1)
        cli.i(longOpt: "imageFile", "File to write image details to", required: true, args: 1)
        cli.d(longOpt: "delimiter", "Data file delimiter (defaults to ,)", required: false, args: 1)
        cli.r(longOpt: "reportFile", "File to write the results of the import to", required: false, args: 1)

        OptionAccessor opt = cli.parse(args)

        if (!opt) {
            cli.usage()
            return
        }

        OPUS_ID = opt.o
        DATA_DIR = opt.f
        IMAGE_FILE = opt.i
        REPORT_FILE = opt.r ?: "report.txt"
        PROFILE_SERVICE_IMPORT_URL = "${opt.p}/import/profile"
        DELIMITER = opt.d ?: ","

        List profiles = []

        println "Processing file..."
        int count = 0

        File imageFile = new File(IMAGE_FILE)
        if (imageFile.exists()) {
            imageFile.delete()
            imageFile.createNewFile()
            imageFile << "scientificName,associatedMedia\n"
        }

        Map<String, List<Integer>> scientificNames = [:]
        Map<Integer, String> invalidLines = [:]

        File report = new File(REPORT_FILE)
        if (report.exists()) {
            report.delete()
            report.createNewFile()
        }

        Map<Integer, String> attributeTitles = loadAttributeTitles()

        Map<Integer, Map<String, List<String>>> taxaAttributes = loadAttributes(attributeTitles)

        Map<Integer, String> contributors = loadContributors()

        Map<Integer, List<String>> taxaContributors = loadTaxaContributors(contributors)

        def csv = parseCsv(new File("${DATA_DIR}/taxa.csv").newReader("utf-8"))
        csv.each { line ->
            if (count++ % 50 == 0) println "Processing taxa line ${count}..."

            // exclude the oceanic volumes (ids 14 and 15)
            if (line.VOLUME_ID == "14" || line.VOLUME_ID== "15") {
                return
            }

            Integer id = line.ID as int
            String scientificName = line.NAME

            List attributes = []
            Map<String, List<String>> attrs = taxaAttributes.get(id)
            if (attrs) {
                attrs.each { k, v ->
                    if (v) {
                        attributes << [title: k, text: v.join("<p/>"), creators: [], stripHtml: false]
                    }
                }
            }

            Map profile = [scientificName: scientificName,
                           nameAuthor: line.AUTHOR,
                           attributes: attributes,
                           authorship: [[category: "Author", text: taxaContributors[id]?.join(", ")]]]

            if (!scientificNames.containsKey(scientificName)) {
                profiles << profile
            }

            scientificNames.get(scientificName, []) << count
        }

        Map opus = [
                opusId  : OPUS_ID,
                profiles: profiles
        ]

        println "Importing ${profiles.size()} profiles..."

        def service = new RESTClient(PROFILE_SERVICE_IMPORT_URL)
        def resp = service.post(body: opus, requestContentType: JSON)

        if (resp.status == 200) {
            if (invalidLines) {
                report << "Invalid lines from source file:\n"
                invalidLines.each {k, v ->
                    report << "\tLine ${k}: ${v}\n"
                }
            }

            if (scientificNames.any {k, v -> v.size() > 1 && k != null}) {
                report << "\n\nDuplicate scientific names (only the first record will be imported): \n"
                scientificNames.each { k, v ->
                    if (v.size() > 1 && k) {
                        report << "\t${k}, on lines ${v}. Line ${v.first()} was imported.\n"
                    }
                }
            }

            int success = 0
            int failed = 0
            if (resp.data.any {k, v -> v != "Success"}) {
                report << "\n\nRecords unable to be saved: \n"
            }
            resp.data.each { k, v ->
                if (v.startsWith("Success")) {
                    success++
                } else {
                    report << "\t${k} Failed: ${v}\n"
                    failed++
                }
            }

            report << "\n\nImported ${success} of ${count} profiles"
        } else {
            println "Import failed with HTTP ${resp.status}"
        }

        println "Import finished. See ${report.absolutePath} for details"
    }

    static Map<Integer, String> loadAttributeTitles() {
        Map<Integer, String> attributeTitles = [:]
        def csv = parseCsv(new File("${DATA_DIR}/properties.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                attributeTitles << [(line.ID as Integer): line.LABEL.trim()]
            } catch (e) {
                println "Failed to extract attribute titles from line [${line}]"
            }
        }

        attributeTitles
    }

    static Map<Integer, Map<String, List<String>>> loadAttributes(Map<Integer, String> attributeTitles) {
        List<String> excludedTitles = ["1", "8", "9", "10", "11", "12", "13", "14", "20", "21", "22"]

        Map<Integer, Map<String, List<String>>> attributes = [:]
        int count = 0
        def csv = parseCsv(new File("${DATA_DIR}/taxa_properties.csv").newReader("utf-8"))
        csv.each { line ->
            if (count++ % 50 == 0) println "Processing attribute line ${count}..."
            try {
                if (excludedTitles.contains(line.PROPERTY_ID)) {
                    return
                }

                if (line.PARENT_ID) {
                    println "Line ${line.ID} (prop id ${line.PROPERTY_ID}) has a parent id of ${line.PARENT_ID}"
                }

                String title = attributeTitles[line.PROPERTY_ID as Integer]

                attributes.get(line.TAXON_ID as Integer, [:]).get(title, []) << "<p>${line.VAL}</p>"
            } catch (e) {
                println "${e.message} - ${line}"
            }
        }

        attributes
    }

    static Map<Integer, String> loadContributors() {
        Map<Integer, String> contributors = [:]

        def csv = parseCsv(new File("${DATA_DIR}/contributors.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                contributors << [(line.ID as Integer): line.NAME_STRING.trim()]
            } catch (e) {
                println "Failed to extract contributors from line [${line}]"
            }
        }

        contributors
    }

    static Map<Integer, List<String>> loadTaxaContributors(Map<Integer, String> contributors) {
        Map<Integer, List<String>> taxaContributors = [:]

        def csv = parseCsv(new File("${DATA_DIR}/taxa_contributors.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                Integer taxa = line.TAXON_ID as Integer
                Integer contr = line.CONTRIBUTOR_ID as Integer
                taxaContributors.get(taxa, []) << contributors[contr]
            } catch (e) {
                println "Failed to extract taxa contributors from line [${line}]"
            }
        }

        taxaContributors
    }

}