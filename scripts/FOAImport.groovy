import groovy.xml.MarkupBuilder
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab('com.xlson.groovycsv:groovycsv:1.0')
@Grab('org.apache.commons:commons-lang3:3.3.2')

import groovyx.net.http.RESTClient

import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.*
import static com.xlson.groovycsv.CsvParser.parseCsv
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

class NSWImport {

    static String OPUS_ID
    static String DATA_DIR
    static boolean INCLUDE_IMAGES = false
    static String REPORT_FILE
    static String PROFILE_SERVICE_IMPORT_URL
    static String DELIMITER


    static void main(args) {
        def cli = new CliBuilder(usage: "groovy FOAImport -f <datadir> -o opusId -p <profileServiceBaseUrl> -d <delimiter default ,> -r <reportfile>")
        cli.f(longOpt: "dir", "source data directory", required: true, args: 1)
        cli.o(longOpt: "opusId", "UUID of the FOA Opus", required: true, args: 1)
        cli.p(longOpt: "profileServiceBaseUrl", "Base URL of the profile service", required: true, args: 1)
        cli.d(longOpt: "delimiter", "Data file delimiter (defaults to ,)", required: false, args: 1)
        cli.r(longOpt: "reportFile", "File to write the results of the import to", required: false, args: 1)

        OptionAccessor opt = cli.parse(args)

        if (!opt) {
            cli.usage()
            return
        }

        OPUS_ID = opt.o
        DATA_DIR = opt.f
        REPORT_FILE = opt.r ?: "report.txt"
        PROFILE_SERVICE_IMPORT_URL = "${opt.p}/import/profile"
        DELIMITER = opt.d ?: ","

        List profiles = []

        println "Processing file..."
        int count = 0

        Map<String, List<Integer>> scientificNames = [:]
        Map<Integer, String> invalidLines = [:]

        File report = new File(REPORT_FILE)
        if (report.exists()) {
            report.delete()
            report.createNewFile()
        }

        Map<String, String> volumes = loadVolumes()

        Map<Integer, String> attributeTitles = loadAttributeTitles()

        Map<Integer, Map<String, List<String>>> taxaAttributes = loadAttributes(attributeTitles)

        Map<Integer, String> contributors = loadContributors()

        Map<Integer, List<String>> taxaContributors = loadTaxaContributors(contributors)

        Map<Integer, Integer> nslConcepts = loadNslConcepts()

        Map<Integer, List<Map<String, String>>> images = loadImages()
        Map<Integer, List<String>> maps = loadMaps()

        List collectionImages = []

        int doubtful = 0

        def csv = parseCsv(new File("${DATA_DIR}/taxa.csv").newReader("utf-8"))
        csv.each { line ->
            // exclude the oceanic volumes (ids 14 and 15)
            if (line.VOLUME_ID == "14" || line.VOLUME_ID== "15") {
                return
            }

            // exclude doubtful names
            if (line.DOUBTFUL_EXCLUDED_OR_OTHER == "Y") {
                doubtful++
                return
            }

            if (count++ % 50 == 0) println "Processing taxa line ${count}..."

            Integer id = line.ID as int
            String scientificName = line.NAME?.trim()

            List connectingTerms = ["subsp.", "var.", "f.", "ser.", "subg.", "sect.", "subsect."]

            String fullName = null

            // full name is the binomial name plus the name author, possibly with an autonym author
            if (line.AUTHOR_AUTONYM) {
                connectingTerms.each {
                    if (scientificName.contains(it)) {
                        fullName = new StringBuilder(scientificName).insert(scientificName.indexOf(it), "${line.AUTHOR_AUTONYM} ").toString()
                    }
                }
            } else {
                fullName = "${scientificName} ${line.AUTHOR}"
            }
            fullName = fullName.trim()

            List attributes = []
            Map<String, List<String>> attrs = taxaAttributes.get(id)
            if (attrs) {
                attrs.each { k, v ->
                    if (v) {
                        attributes << [title: k, text: v.join("<p/>"), creators: [], stripHtml: false]
                    }
                }
            }

            List<Map<String, String>> imgs = images.get(id)
            imgs?.each {
                it.identifier = "http://anbg.gov.au/abrs-archive/Flora_Australia_Online/web_images/vol" + volumes[line.VOLUME_ID] + "/" + it.fileName
                it.scientificName = scientificName
                collectionImages << it
            }
            List<String> mapList = maps.get(id)
            mapList?.each {
                Map metadata = [title: "Distribution Map",
                                identifier: "http://anbg.gov.au/abrs-archive/Flora_Australia_Online/web_images/vol" + volumes[line.VOLUME_ID] + "/" + it,
                                scientificName: scientificName]
                collectionImages << metadata
            }

            Map profile = [scientificName: scientificName,
                           nameAuthor: line.AUTHOR,
                           fullName: fullName,
                           attributes: attributes,
//                           nslNomenclatureIdentifier: nslConcepts.get(id) ?: "",
                           nslNomenclatureMatchStrategy: "NSL_SEARCH",
                           nslNomenclatureMatchData: "NSL_SEARCH",
                           authorship: [[category: "Author", text: taxaContributors[id]?.join(", ")]]]

            if (!scientificNames.containsKey(scientificName.trim().toLowerCase())) {
                profiles << profile
            }

            scientificNames.get(scientificName.trim().toLowerCase(), []) << count
        }

        Map opus = [
                opusId  : OPUS_ID,
                profiles: profiles
        ]

        println "Creating image files..."
        createImageFiles(collectionImages)
if (true) return
        println "Ignoring ${doubtful} doubtful names"
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
            int warnings = 0
            if (resp.data.any {k, v -> v != "Success"}) {
                report << "\n\nRecords unable to be saved: \n"
            }
            resp.data.each { k, v ->
                if (v.startsWith("Success")) {
                    success++
                } else if (v.startsWith("Warning")) {
                    report << "\t${k}: ${v}\n"
                    warnings++
                } else {
                    report << "\t${k} Failed: ${v}\n"
                    failed++
                }
            }

            report << "\n\nImported ${success} of ${count} profiles with ${failed} errors and ${warnings} warnings"
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

    static Map<Integer, List<Map<String, String>>> loadImages() {
        Map<Integer, Map<String, String>> images = [:]

        def csv = parseCsv(new File("${DATA_DIR}/figures.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                images << [(line.ID as Integer): [fileName: line.FILE_NAME,
                                                  title: cleanupText(line.CAPTION),
                                                  creator: cleanupText(line.ILLUSTRATOR)]]
            } catch (e) {
                println "Failed to extract figure from line [${line}]"
            }
        }

        Map<Integer, List<Map<String, String>>> taxaImages = [:]

        csv = parseCsv(new File("${DATA_DIR}/taxa_figures.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                taxaImages.get(line.TAXON_ID as Integer, []) << images[line.FIGURE_ID as Integer]
            } catch (e) {
                println "Failed to extract figure from line [${line}]"
            }
        }

        taxaImages
    }

    static Map<Integer, List<String>> loadMaps() {
        Map<Integer, List<String>> maps = [:]

        def csv = parseCsv(new File("${DATA_DIR}/distribution_maps.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                maps.get(line.TAXON_ID as Integer, []) << line.FILE_NAME
            } catch (e) {
                println "Failed to extract figure from line [${line}]"
            }
        }

        maps
    }

    static Map<String, String> loadVolumes() {
        Map<String, String> volumes = [:]

        def csv = parseCsv(new File("${DATA_DIR}/volumes.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                volumes << [(line.ID): line.NUM]
            } catch (e) {
                println "Failed to extract volume from line [${line}]"
            }
        }

        volumes
    }

    static Map<String, String> loadNslConcepts() {
        Map<String, String> nslConcepts = [:]

        def csv = parseCsv(new File("${DATA_DIR}/nsl_foa_concept.csv").newReader("utf-8"))
        csv.each { line ->
            try {
                nslConcepts << [(line.foa_id as Integer): line.nsl_id as Integer]
            } catch (e) {
                println "Failed to extract nsl from line [${line}]: ${e.message}"
            }
        }

        nslConcepts
    }

    static cleanupText(str) {
        if (str) {
            str = unescapeHtml4(str)
            // preserve line breaks as new lines, remove all other html
            str = str.replaceAll(/<p\/?>|<br\/?>/, "\n").replaceAll(/<.+?>/, "").replaceAll("\\p{Pd}", "-").replaceAll("\u2014", "-").trim()
        }
        return str
    }

    static createImageFiles(List collectionImages) {
        String now = new SimpleDateFormat("yyyyMMdd").format(new Date())

        File dir = new File("foa_images_${now}")
        if (dir.exists()) {
            dir.list().each {
                new File(dir, it).delete()
            }
            dir.delete()
        }
        dir.mkdir()

        File images = new File(dir, "images.csv")
        images << "coreID,identifier,title\n"

        File export = new File(dir, "export.csv")
        export << "dcterms:type,basisOfRecord,catalogNumber,scientificName\n"

        File metadata = new File(dir, "meta.xml")

        int row = 1
        collectionImages.each {
            images.append("\"${row}\",\"${it.identifier}\",\"${it.title}\",\"${it.creator ?: ""}\"\n", "UTF-8")
            export.append("\"Image\",\"HumanObservation\",\"${row}\",\"${it.scientificName}\"\n", "UTF-8")
            row++
        }

        MarkupBuilder xml = new MarkupBuilder(new FileWriter(metadata))

        xml.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        xml.archive(xmlns: "http://rs.tdwg.org/dwc/text/") {
            core(encoding: "UTF-8",
                    linesTerminatedBy: "\\r\\n",
                    fieldsTerminatedBy: ",",
                    fieldsEnclosedBy: "",
                    ignoreHeaderLines: "1",
                    rowTye: "http://rs.tdwg.org/dwc/terms/Occurrence") {
                files() {
                    location("export.csv")
                }
                id(index: 2)
                field(index: 0, term: "http://purl.org/dc/terms/type")
                field(index: 1, term: "http://rs.tdwg.org/dwc/terms/basisOfRecord")
                field(index: 2, term: "http://rs.tdwg.org/dwc/terms/catalogNumber")
                field(index: 3, term: "http://rs.tdwg.org/dwc/terms/scientificName")
            }
            extension(encoding: "UTF-8",
                    linesTerminatedBy: "\\r\\n",
                    fieldsTerminatedBy: ",",
                    fieldsEnclosedBy: '"',
                    ignoreHeaderLines: "1",
                    rowTye: "http://rs.gbif.org/terms/1.0/Image") {
                files() {
                    location("images.csv")
                }
                coreid(index: 0)
                field(index: 1, term: "http://purl.org/dc/terms/identifier")
                field(index: 2, term: "http://purl.org/dc/terms/title")
                field(index: 2, term: "http://purl.org/dc/terms/creator")
            }
        }
    }
}