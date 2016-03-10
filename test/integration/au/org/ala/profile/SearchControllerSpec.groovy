package au.org.ala.profile

import au.org.ala.profile.util.ProfileSortOption
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
        1 * searchService.findByScientificName("sciName", [], ProfileSortOption.getDefault(), false, -1, 0)
    }

    def "findByScientificName should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        controller.params.useWildcard = "true"
        controller.params.offset = 10
        controller.params.opusId = "one,two"
        controller.params.max = 666
        controller.params.scientificName = "sciName"
        controller.params.sortBy = "name"
        controller.findByScientificName()

        then:
        1 * searchService.findByScientificName("sciName", ["one", "two"], ProfileSortOption.NAME, true, 666, 10)
    }

    def "findByClassificationNameAndRank should return 400 BAD REQUEST if no scientificName or taxon are provided"() {
        when:
        controller.params.scientificName = "sciName"
        controller.findByClassificationNameAndRank()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST


        when:
        controller.params.taxon = "taxon"
        controller.findByClassificationNameAndRank()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "findByNameAndTaxonLevel should default the opus list([]), wildcard (false), max (-1) and offset (0) parameters"() {
        when:
        controller.params.scientificName = "sciName"
        controller.params.taxon = "taxon"
        controller.findByClassificationNameAndRank()

        then:
        1 * searchService.findByClassificationNameAndRank("taxon", "sciName", [], ProfileSortOption.getDefault(), -1, 0)
    }

    def "findByClassificationNameAndRank should use the provided values for the opus list, wildcard, max and offset parameters"() {
        when:
        controller.params.useWildcard = "true"
        controller.params.offset = 10
        controller.params.opusId = "one,two"
        controller.params.max = 666
        controller.params.scientificName = "sciName"
        controller.params.taxon = "taxon"
        controller.findByClassificationNameAndRank()

        then:
        1 * searchService.findByClassificationNameAndRank("taxon", "sciName", ["one", "two"], ProfileSortOption.getDefault(), 666, 10)
    }

    def "getRanks should return a 400 BAD REQUEST if no opus id was provided"() {
        when:
        controller.getRanks()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByRank should return 400 BAD REQUEST if no opusId or taxon are provided"() {
        when:
        controller.params.opusId = "opus1"
        controller.groupByRank()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByRank should return 400 BAD REQUEST if no opus id is provided"() {
        when:
        controller.params.taxon = "taxon"
        controller.groupByRank()

        then:
        controller.response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "groupByRank should default the max (-1) and offset (0) parameters"() {
        given:
        save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")

        when:
        controller.params.opusId = "opusId"
        controller.params.taxon = "taxon"
        controller.groupByRank()

        then:
        1 * searchService.groupByRank("opusId", "taxon", null, -1, 0) >> [:]
    }

    def "groupByRank should use the provided values for the opus list, wildcard, max and offset parameters"() {
        given:
        save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")

        when:
        controller.params.offset = 10
        controller.params.opusId = "opusId"
        controller.params.max = 666
        controller.params.taxon = "taxon"
        controller.groupByRank()

        then:
        1 * searchService.groupByRank("opusId", "taxon", null, 666, 10) >> [:]
    }

    def "getImmediateChildren should return a count of children, excluding the profile itself - zero count"() {
        given:
        Opus opus = save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")
        save new Profile(opus: opus, uuid: "profile1", scientificName: "Bleasdalea", rank: "genus", guid: "name1", classification: [
                new Classification(rank: "genus", name: "Bleasdalea")
        ])
        save new Profile(opus: opus, uuid: "profile2", scientificName: "Bleasdalea bleasdalei", rank: "species", guid: "name2", classification: [
                new Classification(rank: "genus", name: "Bleasdalea"),
                new Classification(rank: "species", name: "Bleasdalea bleasdalei")
        ])

        when: "asked for the children of a genus where there is one species with no subspecies"
        controller.params.opusId = "opusId"
        controller.params.rank = "genus"
        controller.params.name = "Bleasdalea"
        controller.getImmediateChildren()

        then: "the result list should contain one profile (the species), and the child count of that profile should be 0 (no subsp.)"
        1 * controller.searchService.getImmediateChildren(_, _, _, _, _, _) >> [[name: "Bleasdalea bleasdalei", rank: "species", guid: "name2"]]
        controller.response.json.size() == 1
        controller.response.json[0].childCount == 0
    }

    def "getImmediateChildren should return a count of children, excluding the profile itself - non-zero count"() {
        given:
        Opus opus = save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")
        save new Profile(opus: opus, uuid: "profile3", scientificName: "Acacia", rank: "genus", guid: "name3", classification: [
                new Classification(rank: "genus", name: "Acacia")
        ])
        save new Profile(opus: opus, uuid: "profile4", scientificName: "Acacia dealbata", rank: "species", guid: "name4", classification: [
                new Classification(rank: "genus", name: "Acacia"),
                new Classification(rank: "species", name: "Acacia dealbata")
        ])
        save new Profile(opus: opus, uuid: "profile5", scientificName: "Acacia dealbata subsp. subalpina", rank: "subspecies", guid: "name5", classification: [
                new Classification(rank: "genus", name: "Acacia"),
                new Classification(rank: "species", name: "Acacia dealbata"),
                new Classification(rank: "subspecies", name: "Acacia dealbata subsp. subalpina")
        ])

        when: "asked for the children of a genus where there is one species with a subspecies"
        controller.params.opusId = "opusId"
        controller.params.rank = "genus"
        controller.params.name = "Acacia"
        controller.getImmediateChildren()

        then: "the result list should contain one profile (the species), and the child count of that profile should be 1 (for the subsp.)"
        1 * controller.searchService.getImmediateChildren(_, _, _, _, _, _) >> [[name: "Acacia dealbata", rank: "species", guid: "name4"]]
        controller.response.json.size() == 1
        controller.response.json[0].childCount == 1
    }

    def "getImmediateChildren should return a count of children, excluding the profile itself even when the taxonomy name != the profile name - zero count"() {
        given:
        Opus opus = save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")
        save new Profile(opus: opus, uuid: "profile1", scientificName: "Bleasdalea", rank: "genus", guid: "name1", classification: [
                new Classification(rank: "genus", name: "Bleasdalea")
        ])
        save new Profile(opus: opus, uuid: "profile2", scientificName: "Gevuina bleasdalei", rank: "species", guid: "name2", classification: [
                new Classification(rank: "genus", name: "Bleasdalea"),
                new Classification(rank: "species", name: "Bleasdalea bleasdalei")
        ])

        when: "asked for the children of a genus where the taxonomy name (Bleasdalea bleasdalei) does not match the profile name (Gevuina bleasdalei)"
        controller.params.opusId = "opusId"
        controller.params.rank = "genus"
        controller.params.name = "Bleasdalea"
        controller.getImmediateChildren()

        then: "the result list should contain one profile (the species), and the child count of that profile should be 0 (no subsp.)"
        1 * controller.searchService.getImmediateChildren(_, _, _, _, _, _) >> [[name: "Bleasdalea bleasdalei", rank: "species", guid: "name2"]]
        controller.response.json.size() == 1
        controller.response.json[0].childCount == 0
    }


    def "getImmediateChildren should return a count of children, excluding the profile itself even when the taxonomy name != the profile name - non-zero count"() {
        given:
        Opus opus = save new Opus(uuid: "opusId", shortName: "opusid", title: "opusName", glossary: new Glossary(), dataResourceUid: "dr1")
        save new Profile(opus: opus, uuid: "profile3", scientificName: "Acacia", rank: "genus", guid: "name3", classification: [
                new Classification(rank: "genus", name: "Acacia")
        ])
        save new Profile(opus: opus, uuid: "profile4", scientificName: "Acacia dealbata", rank: "species", guid: "name4", classification: [
                new Classification(rank: "genus", name: "Acacia"),
                new Classification(rank: "species", name: "Acacia dealbata")
        ])
        save new Profile(opus: opus, uuid: "profile5", scientificName: "Racosperma dealbata subsp. subalpina", rank: "subspecies", guid: "name5", classification: [
                new Classification(rank: "genus", name: "Acacia"),
                new Classification(rank: "species", name: "Acacia dealbata"),
                new Classification(rank: "subspecies", name: "Acacia dealbata subsp. subalpina")
        ])

        when: "asked for the children of a genus where there is one species with a subspecies"
        controller.params.opusId = "opusId"
        controller.params.rank = "genus"
        controller.params.name = "Acacia"
        controller.getImmediateChildren()

        then: "the result list should contain one profile (the species), and the child count of that profile should be 1 (for the subsp.)"
        1 * controller.searchService.getImmediateChildren(_, _, _, _, _, _) >> [[name: "Acacia dealbata", rank: "species", guid: "name4"]]
        controller.response.json.size() == 1
        controller.response.json[0].childCount == 1
    }
}
