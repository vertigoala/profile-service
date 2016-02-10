package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON

class SearchController extends BaseController {
    SearchService searchService

    def search() {
        if (!params.term) {
            badRequest "term is a required parameter"
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            String term = params.term as String
            boolean nameOnly = params.nameOnly?.toBoolean()
            int pageSize = params.pageSize ? params.pageSize as int : -1
            int offset = params.offset ? params.offset as int : 0

            render searchService.search(opusIds, term, offset, pageSize, nameOnly) as JSON
        }
    }

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
                        fullName      : it.fullName,
                        rank          : it.rank,
                        opus          : [uuid: it.opus.uuid, title: it.opus.title, shortName: it.opus.shortName]
                ]
            } as JSON
        }
    }

    def findByTaxonNameAndLevel() {
        if (!params.taxon || !params.scientificName) {
            badRequest "taxon (e.g. phylum, genus, species, etc) and scientificName are a required parameters. You can also optionally supply opusId (comma-separated list of opus ids), max (max records to return), offset (0 based index to start from), recursive (whether to get all subordinate taxa (true) or only the next rank (false) - default is true)."
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            boolean countChildren = params.countChildren ? params.countChildren.equalsIgnoreCase("true") : false
            boolean useWildcard = params.useWildcard ? params.useWildcard.equalsIgnoreCase("true") : false
            boolean recursive = params.recursive ? params.recursive.equalsIgnoreCase("true") : true // default recursive search to true
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0

            List<Profile> profiles = searchService.findByTaxonNameAndLevel(params.taxon, params.scientificName, opusIds, useWildcard, max, startFrom, recursive)

            response.setContentType("application/json")
            render profiles.collect { profile ->
                [
                        profileId     : profile.uuid,
                        guid          : profile.guid,
                        scientificName: profile.scientificName,
                        name          : profile.scientificName,
                        rank          : profile.rank,
                        opus          : [uuid: profile.opus.uuid, title: profile.opus.title, shortName: profile.opus.shortName],
                        childCount    : countChildren ? Profile.withCriteria {
                            eq "opus", profile.opus
                            isNull "archivedDate"
                            ne "uuid", profile.uuid

                            "classification" {
                                eq "rank", "${profile.rank.toLowerCase()}"
                                ilike "name", "${profile.scientificName}"
                            }

                            projections {
                                count()
                            }
                        }[0] : -1
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

    def getImmediateChildren() {
        Opus opus = getOpus()
        int max = params.max ? params.max as int : -1
        int startFrom = params.offset ? params.offset as int : 0

        List children = searchService.getImmediateChildren(opus, params.rank, params.name, max, startFrom)
        response.setContentType("application/json")
        render children.collect { profile ->
            Profile relatedProfile = Profile.findByScientificNameAndGuidAndOpusAndArchivedDateIsNull(profile.name, profile.guid, opus)

            [
                    profileId     : relatedProfile?.uuid,
                    profileName   : relatedProfile?.scientificName,
                    guid          : profile.guid,
                    name          : profile.name,
                    rank          : profile.rank,
                    opus          : [uuid: opus.uuid, title: opus.title, shortName: opus.shortName],
                    childCount    : Profile.withCriteria {
                        eq "opus", opus
                        isNull "archivedDate"
                        if (relatedProfile) {
                            ne "uuid", relatedProfile.uuid
                        }

                        "classification" {
                            eq "rank", "${profile.rank.toLowerCase()}"
                            ilike "name", "${profile.name}"
                        }

                        projections {
                            count()
                        }
                    }[0]
            ]
        } as JSON
    }

    @RequireApiKey
    def reindex() {
        searchService.reindex()

        render (Status.first() as JSON)
    }
}
