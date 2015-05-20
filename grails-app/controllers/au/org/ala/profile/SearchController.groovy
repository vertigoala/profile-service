package au.org.ala.profile

import grails.converters.JSON

class SearchController extends BaseController {
    /*
     * Basic search
     * TODO add a free text search index backed search.
     * http://grails.github.io/grails-data-mapping/mongodb/manual/guide/3.%20Mapping%20Domain%20Classes%20to%20MongoDB%20Collections.html#3.7%20Full%20Text%20Search
     * https://blog.codecentric.de/en/2013/01/text-search-mongodb-stemming/
     *
     */

    SearchService searchService

    def findByScientificName() {
        if (!params.scientificName) {
            badRequest "scientificName is a required parameter. You can also optionally supply opusId (comma-separated list of opus ids), useWildcard (true/false to perform a wildcard search, default = true), max (max records to return), offset (0 based index to start from)."
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            boolean useWildcard = params.useWildcard ? params.useWildcard.equalsIgnoreCase("true") : false
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0

            List<Profile> profiles = searchService.findByScientificName(params.scientificName, opusIds, useWildcard, max, startFrom)

            response.setContentType("application/json")
            render profiles.collect {
                [
                        profileId     : it.uuid,
                        guid          : it.guid,
                        scientificName: it.scientificName,
                        nameAuthor    : it.nameAuthor,
                        opus          : [uuid: it.opus.uuid, title: it.opus.title, shortName: it.opus.shortName]
                ]
            } as JSON
        }
    }

    def findByTaxonNameAndLevel() {
        if (!params.taxon || !params.scientificName) {
            badRequest "taxon (e.g. phylum, genus, species, etc) and scientificName are a required parameters. You can also optionally supply opusId (comma-separated list of opus ids), max (max records to return), offset (0 based index to start from)."
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            boolean useWildcard = params.useWildcard ? params.useWildcard.equalsIgnoreCase("true") : false
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0

            List<Profile> profiles = searchService.findByTaxonNameAndLevel(params.taxon, params.scientificName, opusIds, useWildcard, max, startFrom)

            response.setContentType("application/json")
            render profiles.collect {
                [
                        profileId     : it.uuid,
                        guid          : it.guid,
                        scientificName: it.scientificName,
                        opus          : [uuid: it.opus.uuid, title: it.opus.title, shortName: it.opus.shortName]
                ]
            } as JSON
        }
    }

    def getTaxonLevels() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            Map<String, Integer> result = searchService.getTaxonLevels(opus?.uuid)
            render result as JSON
        }
    }

    def groupByTaxonLevel() {
        if (!params.opusId || !params.taxon) {
            badRequest "opusId and taxon are required parameters. You can also optionally supply max (max records to return) and offset (0 based index to start from)."
        } else {
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0

            Opus opus = getOpus()

            Map<String, Integer> result = searchService.groupByTaxonLevel(opus?.uuid, params.taxon, max, startFrom)

            render result as JSON
        }
    }
}
