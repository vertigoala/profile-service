package au.org.ala.profile

import au.org.ala.web.AuthService


class SearchService extends BaseDataAccessService {

    AuthService authService

    static final List<String> RANKS = ["kingdom", "phylum", "class", "subclass", "order", "family", "genus", "species"]
    static final Integer DEFAULT_MAX_OPUS_SEARCH_RESULTS = 25
    static final Integer DEFAULT_MAX_BROAD_SEARCH_RESULTS = 50


    List<Profile> findByScientificName(String scientificName, List<String> opusIds, boolean useWildcard = true, int max = -1, int startFrom = 0) {
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        List<Opus> opusList = opusIds?.collect{ Opus.findByUuidOrShortName(it, it) }?.dropWhile { it == null }

        if (max == -1) {
            max = opusList ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        // search results from private collections can only be seen by ALA Admins or users registered with the collection
        boolean alaAdmin = authService.userInRole("ROLE_ADMIN")
        String userId = authService.getUserId()

        // if the user is ala admin, do nothing
        // if there is no user, remove all private collections
        // if there is a user, remove private collections unless the user is registered with the collection

        if (!alaAdmin) {
            // join queries are not supported in Mongo, so we need to do this programmatically
            opusList = (opusList ?: Opus.list()).findAll {
                !it?.privateCollection || it?.authorities?.find { auth -> auth.user.userId == userId }
            }
        }

        Profile.withCriteria {
            if (opusList) {
                'in' "opus", opusList
            }

            or {
                ilike "scientificName", "${scientificName}${wildcard}"
                ilike "fullName", "${scientificName}${wildcard}"
            }

            isNull "archivedDate"

            order "scientificName"

            maxResults max
            offset startFrom
        }
    }

    List<Profile> findByTaxonNameAndLevel(String taxon, String scientificName, List<String> opusIds, boolean useWildcard = true, int max = -1, int startFrom = 0, boolean recursive = true) {
        checkArgument taxon
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        List<Opus> opusList = opusIds?.collect{ Opus.findByUuidOrShortName(it, it) }?.dropWhile { it == null }

        if (max == -1) {
            max = opusList ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        String nextRank = null
        if (RANKS.indexOf(taxon) < RANKS.size() - 1 && RANKS.indexOf(taxon) > -1) {
            nextRank = RANKS[RANKS.indexOf(taxon) + 1]
        }

        Profile.withCriteria {
            if (opusList) {
                'in' "opus", opusList
            }

            "classification" {
                eq "rank", "${taxon.toLowerCase()}"
                ilike "name", "${scientificName}${wildcard}"
            }

            if (!recursive) {
                eq "rank", nextRank
            }

            isNull "archivedDate"

            order "scientificName"

            maxResults max
            offset startFrom
        }
    }

    Map<String, Integer> getTaxonLevels(String opusId) {
        checkArgument opusId

        Opus opus = Opus.findByUuid(opusId)

        if (opus) {
            Profile.collection.aggregate([[$match: [opus: opus.id, archivedDate: null]],
                                          [$unwind: '$classification'],
                                          [$group: [_id: [rank: '$classification.rank', name: '$classification.name'], cnt: [$sum: 1]]],
                                          [$group: [_id: '$_id.rank', total: [$sum: 1]]]]
            ).results().iterator().collectEntries {
                [(it._id): it.total]
            }
        } else {
            [:]
        }
    }

    Map<String, Integer> groupByTaxonLevel(String opusId, String taxon, int max = -1, int startFrom = 0) {
        checkArgument opusId
        checkArgument taxon

        Opus opus = Opus.findByUuid(opusId)

        if (opus) {
            String nextRank = null
            if (RANKS.indexOf(taxon) < RANKS.size() - 1 && RANKS.indexOf(taxon) > -1) {
                nextRank = RANKS[RANKS.indexOf(taxon) + 1]
            }

            def result = Profile.collection.aggregate([$match: [opus: opus.id, archivedDate: null]],
                                                      [$unwind: '$classification'],
                                                      [$match: ["classification.rank": "${taxon}"]],
                                                      [$group: [_id: '$classification.name', cnt: [$sum: 1]]],
                                                      [$sort: ["_id": 1]],
                                                      [$skip: startFrom], [$limit: max < 0 ? DEFAULT_MAX_BROAD_SEARCH_RESULTS : max]
            )?.results()

            result.collectEntries {
                [(it.get("_id")): it.get("cnt")]
            }
        } else {
            [:]
        }
    }
}
