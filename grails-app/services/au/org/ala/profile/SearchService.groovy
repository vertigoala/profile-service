package au.org.ala.profile


class SearchService extends BaseDataAccessService {

    static final List<String> RANKS = ["kingdom", "phylum", "clazz", "subclazz", "order", "family", "genus", "species"]
    static final Integer DEFAULT_MAX_OPUS_SEARCH_RESULTS = 10
    static final Integer DEFAULT_MAX_BROAD_SEARCH_RESULTS = 50


    List<Profile> findByScientificName(String scientificName, List<String> opusIds, boolean useWildcard, int max = -1, int startFrom = 0) {
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        List<Opus> opusList = opusIds?.collect{ Opus.findByUuid(it) }?.dropWhile { it == null }

        if (max == -1) {
            max = opusIds ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        Profile.withCriteria {
            if (opusList) {
                'in' "opus", opusList
            }

            ilike "scientificName", "${scientificName}${wildcard}"

            order "scientificName"

            maxResults max
            offset startFrom
        }
    }

    List<Profile> findByNameAndTaxonLevel(String taxon, String scientificName, List<String> opusIds, boolean useWildcard, int max = -1, int startFrom = 0) {
        checkArgument taxon
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        List<Opus> opusList = opusIds?.collect{ Opus.findByUuid(it) }?.dropWhile { it == null }

        if (max == -1) {
            max = opusIds ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        Profile.withCriteria {
            if (opusList) {
                'in' "opus", opusList
            }

            ilike "classification.${taxon.toLowerCase()}", "${scientificName}${wildcard}"

            order "scientificName"

            maxResults max
            offset startFrom
        }
    }

    Map<String, Integer> getTaxonLevels(String opusId) {
        checkArgument opusId

        Opus opus = Opus.findByUuid(opusId)

        if (opus) {
            RANKS.collectEntries {
                [it, Profile.collection.aggregate([[$match: [opus: opus.id, "classification.${it}": [$ne: null]]],
                                                   [$group: [_id: '$classification.' + it, cnt: [$sum: 1]]],
                                                   [$group: [_id: null, total: [$sum: 1]]]]
                ).results().iterator().next().get("total")
                ]
            }
        } else {
            [:]
        }
    }

    Map<String, Integer> groupByTaxonLevel(String opusId, String taxon, int max, int startFrom) {
        checkArgument opusId
        checkArgument taxon

        Opus opus = Opus.findByUuid(opusId)

        if (opus) {
            def result = Profile.collection.aggregate([$match: [opus: opus.id, "classification.${taxon}": [$ne: null]]],
                                                      [$group: [_id: '$classification.' + taxon, cnt: [$sum: 1]]],
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
