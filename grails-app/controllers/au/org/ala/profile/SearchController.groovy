package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import au.org.ala.profile.util.ProfileSortOption
import au.org.ala.profile.util.SearchOptions
import grails.converters.JSON

class SearchController extends BaseController {
    SearchService searchService

    def search() {
        List<String> opusIds = params.opusId?.split(",") ?: []

        String term = params.term as String
        int pageSize = params.pageSize ? params.pageSize as int : -1
        int offset = params.offset ? params.offset as int : 0

        SearchOptions options = new SearchOptions()
        options.nameOnly = params.nameOnly?.toBoolean()
        options.includeArchived = params.includeArchived?.toBoolean()
        options.matchAll = params.matchAll?.toBoolean()
        options.searchAla = params.searchAla?.toBoolean()
        options.searchNsl = params.searchNsl?.toBoolean()
        options.includeNameAttributes = params.includeNameAttributes?.toBoolean()

        render searchService.search(opusIds, term, offset, pageSize, options) as JSON
    }

    def findByScientificName() {
        if (!params.scientificName) {
            badRequest "scientificName is a required parameter. You can also optionally supply opusId (comma-separated list of opus ids), useWildcard (true/false to perform a wildcard search, default = true), max (max records to return), offset (0 based index to start from)."
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            boolean useWildcard = params.useWildcard ? params.useWildcard.equalsIgnoreCase("true") : false
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0
            ProfileSortOption sortBy = ProfileSortOption.byName(params.sortBy) ?: ProfileSortOption.getDefault()

            List<Map> profiles = searchService.findByScientificName(params.scientificName, opusIds, sortBy, useWildcard, max, startFrom)

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

    def findByClassificationNameAndRank() {
        if (!params.taxon || !params.scientificName) {
            badRequest "taxon (e.g. phylum, genus, species, etc) and scientificName are a required parameters. You can also optionally supply opusId (comma-separated list of opus ids), max (max records to return), offset (0 based index to start from), recursive (whether to get all subordinate taxa (true) or only the next rank (false) - default is true)."
        } else {
            List<String> opusIds = params.opusId?.split(",") ?: []

            boolean countChildren = params.countChildren ? params.countChildren.equalsIgnoreCase("true") : false
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0
            ProfileSortOption sortBy = ProfileSortOption.byName(params.sortBy) ?: ProfileSortOption.getDefault()
            List<Map> profiles = searchService.findByClassificationNameAndRank(params.taxon, params.scientificName, opusIds, sortBy, max, startFrom)

            response.setContentType("application/json")
            render profiles.collect { profile ->
                [
                        profileId     : profile.uuid,
                        guid          : profile.guid,
                        scientificName: profile.scientificName,
                        name          : profile.scientificName,
                        rank          : profile.rank,
                        taxonomicOrder: profile.taxonomicOrder,
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

    def getRanks() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            Map<String, Integer> result = searchService.getRanks(opus?.uuid)
            render result as JSON
        }
    }

    def groupByRank() {
        if (!params.opusId || !params.taxon) {
            badRequest "opusId and taxon are required parameters. You can also optionally supply filter (name to filter on), max (max records to return) and offset (0 based index to start from)."
        } else {
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0

            Opus opus = getOpus()

            Map<String, Integer> result = searchService.groupByRank(opus?.uuid, params.taxon, params.filter, max, startFrom)

            render result as JSON
        }
    }

    def getImmediateChildren() {
        if (!params.opusId || !params.rank || !params.name) {
            badRequest "opusId, rank and name are required parameters. max, offset and filter are optional."
        } else {
            Opus opus = getOpus()
            int max = params.max ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0
            String filter = params.filter ?: null

            List children = searchService.getImmediateChildren(opus, params.rank, params.name, filter, max, startFrom)
            response.setContentType("application/json")
            render children.collect { profile ->
                Profile relatedProfile
                if (profile.guid) {
                    relatedProfile = Profile.findByGuidAndOpusAndArchivedDateIsNull(profile.guid, opus)
                } else {
                    relatedProfile = Profile.findByScientificNameAndOpusAndArchivedDateIsNull(profile.name, opus)
                }

                [
                        profileId  : relatedProfile?.uuid,
                        profileName: relatedProfile?.scientificName,
                        guid       : profile.guid,
                        name       : profile.name,
                        rank       : profile.rank,
                        opus       : [uuid: opus.uuid, title: opus.title, shortName: opus.shortName],
                        childCount : Profile.withCriteria {
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
    }

    @RequireApiKey
    def reindex() {

        if (Status.count() == 0) {
            Status status = new Status()
            status.searchReindex = true
            save status
        }

        searchService.reindexAll()

       render (Status.first() as JSON)
    }
}
