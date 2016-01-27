package au.org.ala.profile

import au.org.ala.profile.util.Utils
import com.sun.xml.internal.ws.util.StringUtils
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder

import static org.elasticsearch.index.query.FilterBuilders.*
import static org.elasticsearch.index.query.QueryBuilders.*

import au.org.ala.web.AuthService
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.QueryBuilder

import org.grails.plugins.elasticsearch.ElasticSearchService

class SearchService extends BaseDataAccessService {

    static final List<String> RANKS = ["kingdom", "phylum", "class", "subclass", "order", "family", "genus", "species"]
    static final Integer DEFAULT_MAX_OPUS_SEARCH_RESULTS = 25
    static final Integer DEFAULT_MAX_BROAD_SEARCH_RESULTS = 50
    static final String[] NAME_FIELDS = ["scientificName", "matchedName"]
    static final String[] ALL_FIELDS = ["attribute.text", "scientificName^4", "matchedName^3"]

    AuthService authService
    UserService userService
    BieService bieService
    ElasticSearchService elasticSearchService
    NameService nameService

    Map search(List<String> opusIds, String term, int offset, int pageSize, boolean nameOnly = false) {
        String[] accessibleCollections = getAccessibleCollections(opusIds)?.collect { it.uuid } as String[]

        Map results = [:]
        if (term && accessibleCollections) {
            Map params = [
                    score: true,
                    from: offset,
                    size: pageSize
            ]

            QueryBuilder query = nameOnly ? buildNameSearch(term, accessibleCollections) : buildTextSearch(term, accessibleCollections)

            log.debug(query.toString())

            def rawResults = elasticSearchService.search(query, null, params)

            results.total = rawResults.total
            results.items = rawResults.searchResults.collect { Profile it ->
                [
                        scientificName: it.scientificName,
                        uuid          : it.uuid,
                        guid          : it.guid,
                        rank          : it.rank,
                        primaryImage  : it.primaryImage,
                        opusShortName : it.opus.shortName,
                        opusName      : it.opus.title,
                        opusId        : it.opus.uuid,
                        classification: it.classification ?: [],
                        score         : rawResults.scores[it.id.toString()],
                        description   : it.attributes ? it.attributes.findResults {
                                it.title.name.toLowerCase().contains("description") ? [title: it.title.name, text: Utils.cleanupText(it.text)] : null
                        } : [],
                        otherNames    : it.attributes ? it.attributes.findResults {
                                it.title.name.toLowerCase().contains("name") ? [title: it.title.name, text: Utils.cleanupText(it.text)] : null
                        } : []
                ]
            }

            log.debug("Search for ${term} returned ${results.total} hits")
        }

        results
    }

    /**
     * A name search will look for the term(s) in:
     * <ul>
     *     <li>any of the {@link #NAME_FIELDS} fields
     *     <li>or any Attribute where the title contains 'name'
     * </ul>
     *
     */
    private QueryBuilder buildNameSearch(String term, String[] accessibleCollections) {
        // Try to find any other names associated with the provided name - this will help with searching by synonyms
        Set<String> otherNames = bieService.getOtherNames(term) ?: []
        String alaMatchedName = nameService.matchName(term)?.scientificName
        if (alaMatchedName && term != alaMatchedName) {
            otherNames << alaMatchedName
        }
        String nslMatchedName = nameService.matchNSLName(term)?.scientificName
        if (nslMatchedName && term != nslMatchedName) {
            otherNames << nslMatchedName
        }
        otherNames.remove(term)

        BoolQueryBuilder query = boolQuery()

        // rank matches using the provided text higher than matches based on other names from the BIE
        BoolQueryBuilder providedNameQuery = buildBaseNameSearch(term).boost(3)

        query.should(providedNameQuery)

        if (otherNames) {
            otherNames.each {
                query.should(buildBaseNameSearch(it))
            }
        }

        filteredQuery(query, buildFilter(accessibleCollections))
    }

    private static BoolQueryBuilder buildBaseNameSearch(String term) {
        QueryBuilder attributesWithNames = boolQuery()
                .must(nestedQuery("attributes.title", boolQuery().must(matchQuery("attributes.title.name", "name"))))
                .must(matchQuery("text", term).operator(MatchQueryBuilder.Operator.AND))

        boolQuery()
                .should(termQuery("scientificName.untouched", StringUtils.capitalize(term)).boost(4)) // rank exact matches on the profile name highest of all
                .should(multiMatchQuery(term, NAME_FIELDS).type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX).operator(MatchQueryBuilder.Operator.AND))
                .should(nestedQuery("attributes", attributesWithNames))
    }

    private static FilterBuilder buildFilter(String[] accessibleCollections) {
        boolFilter()
                .must(missingFilter("archivedDate"))
                .must(nestedFilter("opus", boolFilter().must(termsFilter("opus.uuid", accessibleCollections))))
    }

    /**
     * A text search will look for the term(s) in any indexed field
     *
     */
    private static QueryBuilder buildTextSearch(String term, String[] accessibleCollections) {
        QueryBuilder attributesWithNames = boolQuery()
                .must(nestedQuery("attributes.title", boolQuery().must(matchQuery("attributes.title.name", "name"))))
                .must(matchQuery("text", term).operator(MatchQueryBuilder.Operator.AND))

        filteredQuery(boolQuery()
                .should(matchQuery("scientificName.untouched", term).boost(4))
                .should(multiMatchQuery(term, ALL_FIELDS).operator(MatchQueryBuilder.Operator.AND).boost(2))
                .should(multiMatchQuery(term, ALL_FIELDS).operator(MatchQueryBuilder.Operator.OR))
                .should(nestedQuery("attributes", attributesWithNames).boost(3))
                .should(nestedQuery("attributes", boolQuery().must(matchPhrasePrefixQuery("text", term).operator(MatchQueryBuilder.Operator.AND)).boost(2)))
                .should(nestedQuery("attributes", boolQuery().must(matchPhrasePrefixQuery("text", term).operator(MatchQueryBuilder.Operator.OR)))),
                buildFilter(accessibleCollections))
    }

    List<Profile> findByScientificName(String scientificName, List<String> opusIds, boolean useWildcard = true, int max = -1, int startFrom = 0) {
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        if (max == -1) {
            max = opusIds ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        List<Opus> accessibleCollections = getAccessibleCollections(opusIds)

        List<Profile> results

        if (!accessibleCollections && opusIds) {
            println "here2"
            // if the original opusList was not empty but the filtered list is, then the current user does not have permission
            // to view profiles from any of the collections, so return an empty list
            results = []
        } else {
            println "here"
            results = Profile.withCriteria {
                if (accessibleCollections) {
                    'in' "opus", accessibleCollections
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

        results
    }

    List<Profile> findByTaxonNameAndLevel(String taxon, String scientificName, List<String> opusIds, boolean useWildcard = true, int max = -1, int startFrom = 0, boolean recursive = true) {
        checkArgument taxon
        checkArgument scientificName

        String wildcard = "%"
        if (!useWildcard) {
            wildcard = ""
        }

        List<Opus> opusList = opusIds?.findResults { Opus.findByUuidOrShortName(it, it) }

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

    private List<Opus> getAccessibleCollections(List<String> opusIds) {
        List<Opus> opusList = opusIds?.findResults { Opus.findByUuidOrShortName(it, it) }

        // search results from private collections can only be seen by ALA Admins or users registered with the collection
        boolean alaAdmin = authService.userInRole("ROLE_ADMIN")
        String userId = userService.getCurrentUserDetails()?.userId

        // if the user is ala admin, do nothing
        // if there is no user, remove all private collections
        // if there is a user, remove private collections unless the user is registered with the collection

        List filteredOpusList = opusList
        if (!alaAdmin) {
            // join queries are not supported in Mongo, so we need to do this programatically
            filteredOpusList = (opusList ?: Opus.list()).findAll {
                !it?.privateCollection || it?.authorities?.find { auth -> auth.user.userId == userId }
            }
        }

        filteredOpusList
    }
}
