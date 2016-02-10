package au.org.ala.profile

import org.apache.http.HttpStatus

class SearchControllerSpec extends BaseIntegrationSpec {
    SearchController controller
    SearchService searchService

    def setup() {
        controller = new SearchController()

        searchService = Mock(SearchService)
        controller.searchService = searchService
    }

    def "findByScientificName should return 400 BAD REQUEST if no scientificName was provided"() {
        when:
        controller.findByScientificName()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "findByScientificName should default the opus list([]), wildcard (false), max (-1) and offset (0) parameters"() {
        when:
        controller.params.scientificName = "sciName"
        controller.findByScientificName()

        then:
        1 * searchService.findByScientificName("sciName", [], false, -1, 0)
    }

    def "findByScientificName should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        controller.params.useWildcard = "true"
        controller.params.offset = 10
        controller.params.opusId = "one,two"
        controller.params.max = 666
        controller.params.scientificName = "sciName"
        controller.findByScientificName()

        then:
        1 * searchService.findByScientificName("sciName", ["one", "two"], true, 666, 10)
    }

    def "findByTaxonNameAndLevel should return 400 BAD REQUEST if no scientificName or taxon are provided"() {
        when:
        controller.params.scientificName = "sciName"
        controller.findByTaxonNameAndLevel()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST


        when:
        controller.params.taxon = "taxon"
        controller.findByTaxonNameAndLevel()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "findByNameAndTaxonLevel should default the opus list([]), wildcard (false), max (-1) and offset (0) parameters"() {
        when:
        controller.params.scientificName = "sciName"
        controller.params.taxon = "taxon"
        controller.findByTaxonNameAndLevel()

        then:
        1 * searchService.findByTaxonNameAndLevel("taxon", "sciName", [], -1, 0)
    }

    def "findByTaxonNameAndLevel should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        controller.params.useWildcard = "true"
        controller.params.offset = 10
        controller.params.opusId = "one,two"
        controller.params.max = 666
        controller.params.scientificName = "sciName"
        controller.params.taxon = "taxon"
        controller.findByTaxonNameAndLevel()

        then:
        1 * searchService.findByTaxonNameAndLevel("taxon", "sciName", ["one", "two"], 666, 10)
    }

    def "getTaxonLevels should return a 400 BAD REQUEST if no opus id was provided"() {
        when:
        controller.getTaxonLevels()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByTaxonLevel should return 400 BAD REQUEST if no opusId or taxon are provided"() {
        when:
        controller.params.opusId = "opus1"
        controller.groupByTaxonLevel()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByTaxonLevel should return 400 BAD REQUEST if no opus id is provided"() {
        when:
        controller.params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByTaxonLevel should default the max (-1) and offset (0) parameters"() {
        given:
        save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")

        when:
        controller.params.opusId = "opusId"
        controller.params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        1 * searchService.groupByTaxonLevel("opusId", "taxon", -1, 0) >> [:]
    }

    def "groupByTaxonLevel should use the provided values for the opus list, wildcard, max and offset parameters"() {
        given:
        save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")

        when:
        controller.params.offset = 10
        controller.params.opusId = "opusId"
        controller.params.max = 666
        controller.params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        1 * searchService.groupByTaxonLevel("opusId", "taxon", 666, 10) >> [:]
    }
}
