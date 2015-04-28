package au.org.ala.profile

import grails.test.mixin.TestFor
import org.apache.http.HttpStatus
import spock.lang.Specification

@TestFor(SearchController)
class SearchControllerSpec extends Specification {
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
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "findByScientificName should default the opus list([]), wildcard (false), max (-1) and offset (0) parameters"() {
        when:
        params.scientificName = "sciName"
        controller.findByScientificName()

        then:
        1 * searchService.findByScientificName("sciName", [], false, -1, 0)
    }

    def "findByScientificName should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        params.useWildcard = "true"
        params.offset = 10
        params.opusId = "one,two"
        params.max = 666
        params.scientificName = "sciName"
        controller.findByScientificName()

        then:
        1 * searchService.findByScientificName("sciName", ["one", "two"], true, 666, 10)
    }

    def "findByNameAndTaxonLevel should return 400 BAD REQUEST if no scientificName or taxon are provided"() {
        when:
        params.scientificName = "sciName"
        controller.findByNameAndTaxonLevel()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST


        when:
        params.taxon = "taxon"
        controller.findByNameAndTaxonLevel()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "findByNameAndTaxonLevel should default the opus list([]), wildcard (false), max (-1) and offset (0) parameters"() {
        when:
        params.scientificName = "sciName"
        params.taxon = "taxon"
        controller.findByNameAndTaxonLevel()

        then:
        1 * searchService.findByNameAndTaxonLevel("taxon", "sciName", [], false, -1, 0)
    }

    def "findByNameAndTaxonLevel should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        params.useWildcard = "true"
        params.offset = 10
        params.opusId = "one,two"
        params.max = 666
        params.scientificName = "sciName"
        params.taxon = "taxon"
        controller.findByNameAndTaxonLevel()

        then:
        1 * searchService.findByNameAndTaxonLevel("taxon", "sciName", ["one", "two"], true, 666, 10)
    }

    def "getTaxonLevels should return a 400 BAD REQUEST if no opus id was provided"() {
        when:
        controller.getTaxonLevels()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByTaxonLevel should return 400 BAD REQUEST if no opusId or taxon are provided"() {
        when:
        params.opusId = "opus1"
        controller.groupByTaxonLevel()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST


        when:
        params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByTaxonLevel should default the max (-1) and offset (0) parameters"() {
        when:
        params.opusId = "opusId"
        params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        1 * searchService.groupByTaxonLevel("opusId", "taxon", -1, 0)
    }

    def "groupByTaxonLevel should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        params.offset = 10
        params.opusId = "opusId"
        params.max = 666
        params.taxon = "taxon"
        controller.groupByTaxonLevel()

        then:
        1 * searchService.groupByTaxonLevel("opusId", "taxon", 666, 10)
    }
}
