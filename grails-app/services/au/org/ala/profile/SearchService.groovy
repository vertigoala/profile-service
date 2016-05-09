package au.org.ala.profile

import static org.elasticsearch.index.query.MatchQueryBuilder.Operator.AND
import static org.elasticsearch.index.query.MatchQueryBuilder.Operator.OR
import au.org.ala.profile.util.ProfileSortOption
import au.org.ala.profile.util.Utils
import com.mongodb.BasicDBObject
import com.mongodb.MapReduceCommand
import com.mongodb.MapReduceOutput
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.gbif.ecat.voc.Rank
import org.springframework.scheduling.annotation.Async

import static org.elasticsearch.index.query.FilterBuilders.*
import static org.elasticsearch.index.query.QueryBuilders.*

import au.org.ala.web.AuthService
import org.elasticsearch.index.query.FilterBuilder
import org.elasticsearch.index.query.QueryBuilder

import org.grails.plugins.elasticsearch.ElasticSearchService

/**
 * See http://noamt.github.io/elasticsearch-grails-plugin/docs/index.html for elastic search plugin API doco
 */
class SearchService extends BaseDataAccessService {
    static final String UNKNOWN_RANK = "unknown" // used for profiles with no rank/classification
    static final Integer DEFAULT_MAX_CHILDREN_RESULTS = 15
    static final Integer DEFAULT_MAX_OPUS_SEARCH_RESULTS = 25
    static final Integer DEFAULT_MAX_BROAD_SEARCH_RESULTS = 50

    AuthService authService
    UserService userService
    BieService bieService
    ElasticSearchService elasticSearchService
    NameService nameService

    Map search(List<String> opusIds, String term, int offset, int pageSize, boolean nameOnly = false, boolean matchAll = false, boolean includeArchived = false) {
        String[] accessibleCollections = getAccessibleCollections(opusIds)?.collect { it.uuid } as String[]

        Map results = [:]
        if (term && accessibleCollections) {
            Map params = [
                    score: true,
                    from : offset,
                    size : pageSize
            ]

            QueryBuilder query = nameOnly ? buildNameSearch(term, accessibleCollections, includeArchived) : buildTextSearch(term, accessibleCollections, matchAll, includeArchived)
            log.debug(query)
            long start = System.currentTimeMillis()
            def rawResults = elasticSearchService.search(query, null, params)
            log.debug("${nameOnly ? 'name' : 'text'} search for ${term} took ${System.currentTimeMillis() - start}ms and returned ${rawResults.total} results")

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
                        archivedDate  : it.archivedDate,
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
        }

        results
    }

    /**
     * A name search will look for the term(s) in:
     * <ul>
     *     <li>the scientificName (aka profileName)
     *     <li>the matchedName.scientificName
     *     <li>the archivedWithName name (if includeArchived = true)
     *     <li>or any Attribute where the title contains 'name'
     * </ul>
     *
     */
    private QueryBuilder buildNameSearch(String term, String[] accessibleCollections, boolean includeArchived = false) {
        Set<String> otherNames = bieService.searchForPossibleMatches(term) ?: []
        String alaMatchedName = nameService.matchName(term)?.scientificName
        if (alaMatchedName && term != alaMatchedName) {
            otherNames << alaMatchedName
        }

        // make sure the provided term is not in the list of alternate names (case insensitive)
        term = otherNames.find { it?.equalsIgnoreCase(term) } ?: term
        otherNames.remove(term)

        BoolQueryBuilder query = boolQuery()

        // rank matches using the provided text higher than matches based on other names from the BIE
        QueryBuilder providedNameQuery = buildBaseNameSearch(term, includeArchived).boost(3)

        query.should(providedNameQuery)

        if (otherNames) {
            otherNames.each {
                query.should(buildBaseNameSearch(it, includeArchived))
            }
        }

        filteredQuery(query, buildFilter(accessibleCollections, includeArchived))
    }

    private static QueryBuilder buildBaseNameSearch(String term, boolean includeArchived = false) {
        QueryBuilder attributesWithNames = getNameAttributeQuery(term)

        term = term.toLowerCase()

        QueryBuilder query = boolQuery()
                // rank exact matches on the profile name highest of all
                .should(termQuery("scientificNameLower", term).boost(4))
                // exact match on either the scientific or full MATCHED name (profile name might be different)
                .should(termQuery("matchedNameLower", term))
                // match any attribute that is considered a 'name' attribute (e.g. common, vernacular, indigenous names etc)
                .should(nestedQuery("attributes", attributesWithNames))

        if (includeArchived) {
            // rank exact matches on the profile name at the time it was archived the same way as we rank the scientificName
            query.should(termQuery("archivedNameLower", term).boost(4))
        }

        query
    }

    private static QueryBuilder getNameAttributeQuery(String term) {
        // a name attribute is one where the attribute title contains the word 'name'
        boolQuery()
                .must(nestedQuery("attributes.title", boolQuery().must(matchQuery("attributes.title.name", "name"))))
                .must(matchQuery("text", term).operator(AND))
    }

    private static FilterBuilder buildFilter(String[] accessibleCollections, boolean includeArchived = false) {
        FilterBuilder filter = boolFilter()
        if (!includeArchived) {
            filter.must(missingFilter("archivedDate"))
        }

        filter.must(nestedFilter("opus", boolFilter().must(termsFilter("opus.uuid", accessibleCollections))))

        filter
    }

    /**
     * A text search will look for the term(s) in any indexed field
     *
     */
    private static QueryBuilder buildTextSearch(String term, String[] accessibleCollections, boolean matchAll = true, boolean includeArchived = false) {
        MatchQueryBuilder.Operator operator = AND
        if (!matchAll) {
            operator = OR
        }

        QueryBuilder attributesWithNames = getNameAttributeQuery(term)

        QueryBuilder query = boolQuery()
        if (includeArchived) {
            // rank exact matches on the profile name at the time it was archived the same way as we rank the scientificName
            query.should(matchQuery("archivedWithName.untouched", term).boost(4))
        }

        query.should(matchQuery("scientificName", term).boost(4))
        query.should(nestedQuery("matchedName", boolQuery().must(matchQuery("matchedName.scientificName", term).operator(AND))))
        query.should(nestedQuery("attributes", attributesWithNames).boost(3)) // score name-related attributes higher
        query.should(nestedQuery("attributes", boolQuery().must(matchQuery("text", term).operator(operator))))
        query.should(nestedQuery("attributes", boolQuery().must(matchPhrasePrefixQuery("text", term).operator(operator))))

        filteredQuery(query, buildFilter(accessibleCollections, includeArchived))
    }

    List<Map> findByScientificName(String scientificName, List<String> opusIds, ProfileSortOption sortBy = ProfileSortOption.getDefault(), boolean useWildcard = true, int max = -1, int startFrom = 0) {
        checkArgument scientificName
        scientificName = Utils.sanitizeRegex(scientificName)

        String wildcard = ".*"
        if (!useWildcard) {
            wildcard = '\\s*$' // regex end of line to ensure full word matching
        }

        List<Opus> accessibleCollections = getAccessibleCollections(opusIds)
        Map<Long, Opus> opusMap = accessibleCollections?.collectEntries { [(it.id): it] }

        if (max == -1) {
            max = accessibleCollections ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        List<Map> results
        if (!accessibleCollections && opusIds) {
            // if the original opusList was not empty but the filtered list is, then the current user does not have permission
            // to view profiles from any of the collections, so return an empty list
            results = []
        } else {
            Map criteria = [:]
            criteria << [archivedDate: null]
            if (accessibleCollections) {
                criteria << [opus: [$in: accessibleCollections*.id]]
            }

            // using regex to perform a case-insensitive match on EITHER the scientificName OR fullName
            criteria << [$or: [
                    [scientificName: [$regex: /^${scientificName}${wildcard}/, $options: "i"]],
                     [fullName      : [$regex: /^${scientificName}${wildcard}/, $options: "i"]]
            ]]

            // Create a projection containing the commonly used Profile attributes, and calculated fields 'unknownRank'
            // and 'rankOrder' as required for taxonomic sorting
            Map projection = constructProfileSearchProjection()

            Map sort = constructSortCriteria(sortBy)

            // Using a MongoDB Aggregation instead of a GORM Criteria query so we can take advantage of the ability to
            // calculate derived properties in the projection, and sort based on the contents of an array
            def aggregation = Profile.collection.aggregate([
                    [$match: criteria],
                    [$project: projection],
                    [$sort: sort],
                    [$skip: startFrom], [$limit: max]
            ])

            int order = 0;
            results = aggregation.results().collect {
                Opus opus = opusMap ? opusMap[it.opus] : Opus.get(it.opus)

                [
                        uuid          : it.uuid,
                        guid          : it.guid,
                        scientificName: it.scientificName,
                        nameAuthor    : it.nameAuthor,
                        fullName      : it.fullName,
                        rank          : it.rank,
                        opus          : [uuid: opus.uuid, title: opus.title, shortName: opus.shortName],
                        taxonomicOrder: order++
                ]
            }
        }

        results
    }

    /**
     * Find all descendants of the specified taxon, NOT including the taxon itself. For example, rank = genus and
     * scientificName = Acacia should list all Acacia species/subspecies/varieties/etc, but should NOT include Acacia
     * itself.
     *
     * @param rank The rank of the taxon to find the descendants of
     * @param scientificName The name of the taxon to find the descendants of
     * @param opusIds List of opusIds to limit the search to. If null or empty, all collections will be searched
     * @param sortBy Sort option
     * @param max Maximum number of results to return
     * @param startFrom The 0-based offset to start the results from (for paging)
     * @return List of descendant taxa of the specified Rank/ScientificName
     */
    List<Map> findByClassificationNameAndRank(String rank, String scientificName, List<String> opusIds, ProfileSortOption sortBy = ProfileSortOption.getDefault(), int max = -1, int startFrom = 0) {
        checkArgument rank
        checkArgument scientificName

        scientificName = Utils.sanitizeRegex(scientificName)

        List<Opus> opusList = opusIds?.findResults { Opus.findByUuidOrShortName(it, it) }
        Map<Long, Opus> opusMap = opusList?.collectEntries { [(it.id): it] }

        if (max == -1) {
            max = opusList ? DEFAULT_MAX_OPUS_SEARCH_RESULTS : DEFAULT_MAX_BROAD_SEARCH_RESULTS
        }

        Map matchCriteria = [archivedDate: null]
        matchCriteria << ["scientificName": [$regex: /^(?!(${scientificName})$)/, $options: "i"]] // case insensitive NOT condition

        if (opusList) {
            matchCriteria << [opus: [$in: opusList*.id]]
        }

        if (UNKNOWN_RANK.equalsIgnoreCase(rank)) {
            matchCriteria << [rank: null]
            matchCriteria << [classification: null]
        } else {
            // using regex to perform a case-insensitive match on the 'rank' and 'name' attributes of any element in the
            // Classification list
            matchCriteria << [classification: [$elemMatch: [
                    'rank': rank.toLowerCase(),
                    'name': [$regex: /^${scientificName}/, $options: "i"]]
            ]]
        }

        // Create a projection containing the commonly used Profile attributes, and calculated fields 'unknownRank'
        // and 'rankOrder' as required for taxonomic sorting
        Map projection = constructProfileSearchProjection()

        Map sort = constructSortCriteria(sortBy)

        // Using a MongoDB Aggregation instead of a GORM Criteria query so we can take advantage of the ability to
        // calculate derived properties in the projection, and sort based on the contents of an array
        def aggregation = Profile.collection.aggregate([
                [$match: matchCriteria],
                [$project: projection],
                [$sort: sort],
                [$skip: startFrom], [$limit: max]
        ])

        int order = 0;
        aggregation.results().collect {
            Opus opus = opusMap ? opusMap[it.opus] : Opus.get(it.opus)

            [
                    uuid          : it.uuid,
                    guid          : it.guid,
                    scientificName: it.scientificName,
                    rank          : it.rank,
                    opus          : [uuid: opus.uuid, title: opus.title, shortName: opus.shortName],
                    taxonomicOrder: order++
            ]
        }
    }

    List getImmediateChildren(Opus opus, String topRank, String topName, String filter = null, int max = -1, int startFrom = 0) {
        checkArgument topRank
        checkArgument topName

        filter = Utils.sanitizeRegex(filter)?.trim()?.toLowerCase()

        List results = []
        MapReduceOutput hierarchyView = null

        try {
            // Find all the classification item that is exactly 1 item below the specified topRank.
            // This will find all immediate children of the specified rank, regardless of whether they in turn have
            // children, or of whether there is a corresponding profile for the child.
            String mapFunction = """function map() {
                           var filter = '${filter ?: ''}';

                           if (typeof this.classification != 'undefined' && this.classification.length > 0) {
                                for (var i = 0; i < this.classification.length; i++) {
                                    if (this.classification[i].rank.toLowerCase() == '${topRank.toLowerCase()}' &&
                                        this.classification[i].name.toLowerCase() == '${topName.toLowerCase()}' &&
                                        i + 1 < this.classification.length) {
                                        if (!filter || filter.length == 0 || this.classification[i + 1].name.toLowerCase().indexOf(filter) > -1) {
                                            emit(this.classification[i + 1].rank, this.classification[i + 1]);
                                        }
                                    }
                                }
                           }
                       }"""

            // Group all taxa of the same rank into a set of unique names.
            // MongoDB can invoke the reduce function more than once for the same key. In this case, the previous output
            // from the reduce function for that key will become one of the input values to the next reduce function
            // invocation for that key, so we need to ensure that we are not nesting the resulting lists.
            String reduceFunction = """function reduce(rank, classification) {
                            var rankList = ['${Rank.values().collect { it.name().toLowerCase() }.join("','")}'];
                            var uniqueNames = [];
                            var uniqueClassifications = [];

                            classification.forEach(function (cl) {
                                if (cl.hasOwnProperty(rank)) {
                                    cl[rank].forEach( function (cl2) {
                                        if (uniqueNames.indexOf(cl2.name) == -1) {
                                            if (typeof cl2.rank != 'undefined' && cl2.rank) {
                                                cl2.rankOrder = rankList.indexOf(cl2.rank.toLowerCase());
                                            } else {
                                                cl2.rankOrder = -1;
                                            }
                                            uniqueClassifications.push(cl2)
                                            uniqueNames.push(cl2.name);
                                        }
                                    });
                                } else if (uniqueNames.indexOf(cl.name) == -1) {
                                    if (typeof cl.rank != 'undefined' && cl.rank) {
                                        cl.rankOrder = rankList.indexOf(cl.rank.toLowerCase());
                                    } else {
                                        cl.rankOrder = -1;
                                    }
                                    uniqueClassifications.push(cl);
                                    uniqueNames.push(cl.name);
                                }
                            });

                            var result = {};
                            result[rank] = uniqueClassifications;
                            return result;
                       }"""

            // Removes one level of nesting: the reduce function must return a single object (not an array), so we return
            // an object with a key for each rank, and the list of entries as the value. In the finalize method, we then
            // strip that nested structure back out, so we are left with the standard mongo structure for MR results:
            // e.g. { _id: 'family', value: [{rank: a, rankOrder: 1, name: a, guid: a}, ...] } (this is the doc structure
            // stored in the output collection, NOT the structure passed to the finalise function).
            //
            // If there is only 1 item in each map output from the map function, then the reduce function is NOT called,
            // so the input to the finalize function will be the 'raw' output map from the map function, i.e.:
            // {rank: a, rankOrder: 1, name: a, guid: a}.
            // If the reduce function WAS called, then the input to the finalise function will be in the form:
            // {a: [{rank: a, rankOrder: 1, name: a, guid: a}, ...]}
            String finalizeFunction = """function finalize(key, reducedValue) {
                            var flattened = null;

                            if (reducedValue.hasOwnProperty(key)) {
                                flattened = reducedValue[key];
                            } else {
                                flattened = [reducedValue];
                            }

                            return flattened;
                        }"""

            BasicDBObject query = new BasicDBObject()
            query.put("opus", opus.id)

            MapReduceCommand command = new MapReduceCommand(Profile.collection, mapFunction, reduceFunction,
                    UUID.randomUUID().toString().replaceAll("-", ""), MapReduceCommand.OutputType.REPLACE, query)
            command.setFinalize(finalizeFunction)

            hierarchyView = Profile.collection.mapReduce(command)

            // Use an aggregation to extract the individual classification entries from the standard mongo 'value' object
            // and sort them by rank then name. The results are then flattened into a single list of classification entries
            results = hierarchyView.getOutputCollection().aggregate([
                    [$unwind: '$value'],
                    [$sort: ["value.rankOrder": 1, "value.name": 1]],
                    [$skip: startFrom], [$limit: max < 0 ? DEFAULT_MAX_CHILDREN_RESULTS : max]
            ])?.results()?.collect { it.value }?.flatten()

            // drop the temporary collection created during the map reduce process to clean up
        } finally {
            hierarchyView?.drop()
        }

        results
    }

    Map<String, Integer> getRanks(String opusId) {
        checkArgument opusId

        Opus opus = Opus.findByUuid(opusId)

        Map groupedRanks = [:]

        if (opus) {
            groupedRanks = Profile.collection.aggregate([[$match: [opus: opus.id, archivedDate: null]],
                                                         [$unwind: '$classification'],
                                                         [$group: [_id: [rank: '$classification.rank', name: '$classification.name'], cnt: [$sum: 1]]],
                                                         [$group: [_id: '$_id.rank', total: [$sum: 1]]]]
            ).results().iterator().collectEntries {
                [(it._id): it.total]
            }

            groupedRanks[UNKNOWN_RANK] = Profile.countByOpusAndArchivedDateIsNullAndRankIsNullAndClassificationIsNull(opus)
        }

        groupedRanks
    }

    Map<String, Integer> groupByRank(String opusId, String taxon, String filter = null, int max = -1, int startFrom = 0) {
        checkArgument opusId
        checkArgument taxon

        Opus opus = Opus.findByUuid(opusId)

        filter = "${Utils.sanitizeRegex(filter)}.*"

        Map groupedTaxa = [:]
        if (opus) {
            if (!taxon || UNKNOWN_RANK.equalsIgnoreCase(taxon)) {
                groupedTaxa[UNKNOWN_RANK] = Profile.countByOpusAndArchivedDateIsNullAndRankIsNullAndClassificationIsNull(opus)
            } else {
                def result = Profile.collection.aggregate([$match: [opus: opus.id, archivedDate: null]],
                        [$unwind: '$classification'],
                        [$match: ["classification.rank": taxon, "classification.name": [$regex: /^${filter}/, $options: "i"], "rank": [$ne: taxon]]],
                        [$group: [_id: '$classification.name', cnt: [$sum: 1]]],
                        [$sort: ["_id": 1]],
                        [$skip: startFrom], [$limit: max < 0 ? DEFAULT_MAX_BROAD_SEARCH_RESULTS : max]
                )?.results()

                groupedTaxa = result.collectEntries {
                    [(it.get("_id")): it.get("cnt")]
                }
            }
        }

        groupedTaxa
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
            // join queries are not supported in Mongo, so we need to do this programmatically
            filteredOpusList = (opusList ?: Opus.list()).findAll {
                !it?.privateCollection || it?.authorities?.find { auth -> auth.user.userId == userId }
            }
        }

        filteredOpusList
    }

    /**
     * Sort either alphabetically on scientificName, or 'taxonomically' so that species are always listed after
     * their genus, genera are always listed after their family, etc etc. This relies on the rankOrder and
     * unknownRank properties created by the #constructProfileSearchProjection() method.
     */
    private static Map constructSortCriteria(ProfileSortOption sortBy) {
        sortBy = sortBy ?: ProfileSortOption.getDefault()
        Map sort

        if (sortBy == ProfileSortOption.NAME) {
            sort = ['scientificName': 1]
        } else {
            sort = ['unknownRank': 1, 'classification.name': 1, 'rankOrder': 1, 'scientificName': 1]
        }

        sort
    }

    /**
     * Constructs a MongoDB Aggregation $project clause which will include an 'unknownRank' flag for profiles without a
     * known taxonomy, and a 'rankOrder' attribute which will be the index order of the profile's rank property within
     * the GBIG org.gbif.ecat.voc.Rank enumeration or 999 for profiles with no rank (this can be used for sorting by taxonomy).
     *
     * The other fields in the projection are commonly required properties of the Profile (name, rank, id, authors, etc).
     */
    private static Map constructProfileSearchProjection() {
        Map projection = [rankOrder: [:]]
        def previousStep = projection.rankOrder

        // Note: the if-then-else map form of the $cond aggregation operation resulted in an unexplained StackOverflowError
        // when running against MongoDB 2.6 and 3.0.0 on Ubuntu 14.04, despite working correctly on a Mac.
        Rank.values().eachWithIndex { rank, index ->
            List cond = [['$eq': ['$rank', rank.name().toLowerCase()]], index, []]
            previousStep << [$cond: cond]
            previousStep = cond[2]
        }
        previousStep << 999

        projection << [unknownRank: [$cond: [[$eq: [[$ifNull: ['$rank', null]], null]], 1, 0]]]
        projection << [uuid: 1]
        projection << [guid: 1]
        projection << [scientificName: 1]
        projection << [classification: 1]
        projection << [rank: 1]
        projection << [fullName: 1]
        projection << [nameAuthor: 1]
        projection << [opus: 1]

        projection
    }

    @Async
    def reindex() {
        Status status = Status.list()[0]
        status.searchReindex = true
        save status

        long start = System.currentTimeMillis()

        log.warn("Deleting existing index...")
        elasticSearchService.unindex(Profile)

        log.warn("Recreating search index...")
        elasticSearchService.index(Profile)

        int time = System.currentTimeMillis() - start

        status.searchReindex = false
        status.lastReindexDuration = time
            save status

        log.warn("Search re-index complete in ${time} milliseconds")
    }
}
