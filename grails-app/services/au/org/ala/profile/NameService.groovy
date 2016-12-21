package au.org.ala.profile

import au.org.ala.names.search.HomonymException
import au.org.ala.names.search.SearchResultException
import au.org.ala.ws.service.WebService
import grails.converters.JSON
import org.apache.commons.lang3.StringUtils

import static com.xlson.groovycsv.CsvParser.parseCsv
import static au.org.ala.profile.util.Utils.enc
import static au.org.ala.profile.util.Utils.isSuccessful

import au.org.ala.profile.util.NSLNomenclatureMatchStrategy
import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.model.NameSearchResult
import au.org.ala.names.search.ALANameSearcher
import groovy.json.JsonSlurper
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService extends BaseDataAccessService {

    static final String NSL_APC_PRODUCT = "AOC"

    WebService webService

    def grailsApplication
    ALANameSearcher nameSearcher

    @PostConstruct
    def init() {
        nameSearcher = new ALANameSearcher("${grailsApplication.config.name.index.location}")
    }

    Map matchName(String name, Map<String, String> classification = [:], String manuallyMatchedGuid = null) throws SearchResultException {
        LinnaeanRankClassification rankClassification = new LinnaeanRankClassification()
        rankClassification.setScientificName(name)
        classification.kingdom = "Plantae"
        populateClassification(classification, rankClassification)

        Map match
        NameSearchResult result

        if (manuallyMatchedGuid) {
            result = nameSearcher.searchForRecordByLsid(manuallyMatchedGuid)
        } else {
            result = nameSearcher.searchForAcceptedRecordDefaultHandling(rankClassification, true, false)
            log.debug "nameSearch result = ${result}"
        }

        if (result) {
            match = extractMatchDetails(name, result)
        } else {
            // There was no match using the default ALA rules. There are some cases (e.g. homonyms) where some assumptions
            // can be made about the possible matches that are not covered by the ALA Name Matcher.

            try {
                List<NameSearchResult> matches = nameSearcher.searchForRecords(name, null, rankClassification, 100, true, true)
                match = searchPotentialMatches(name, matches) // internal method
            } catch (HomonymException e) {
                log.info "HomonymException = ${e.results as JSON}"
                match = searchPotentialMatches(name, e.results) // internal method

                // if we can't determine exact match then throw exception up the stack
                if (!match) {
                    throw e
                }
            } catch (Exception e) {
                log.warn("Name matching exception thrown when attempting to match ${name}. No match will be returned.", e)
                match = null
            }
        }

        log.debug("${name} was matched to ${match?.scientificName}")

        match
    }

    private Map searchPotentialMatches(String name, List<NameSearchResult> matches) {
        Map match = null

        if (matches) {
            List<NameSearchResult> matchesWithRank = matches.findAll { it.rank }
            if (matchesWithRank.size() == 1) {
                match = extractMatchDetails(name, matchesWithRank[0])
            } else {
                List<NameSearchResult> exactMatchOnName = matches.findAll {
                    it.rankClassification.scientificName.toLowerCase().trim() == name.toLowerCase().trim()
                }
                if (exactMatchOnName.size() == 1) {
                    match = extractMatchDetails(name, exactMatchOnName[0])
                }
            }
        }

        match
    }

    private extractMatchDetails(String name, NameSearchResult result) {
        Map match = [
                guid          : result.lsid,
                scientificName: result.getRankClassification().getScientificName(),
                nameAuthor    : result.getRankClassification().getAuthorship(),
                rank          : result.rank?.rank
        ]

        // Autonym workaround. Autonyms have the author name in the middle, and the name service does not currently
        // (as of 23/6/15) always return the author, even though it may match the name.
        // Therefore, we try to find the author name by comparing the provided name with the matched name, and if
        // they are different, but have the same start and end, then we assume that the difference is the author name.
        // e.g. "Acacia dealbata Link subsp. dealbata" is an autonym, but "Acacia dealbata subsp. subalpina Tindale & Kodela" is a subspecies.
        if (match.nameAuthor == null && name != match.scientificName) {
            List suppliedName = name.split(" ")
            List matchedName = match.scientificName.split(" ")
            if (suppliedName.first() == matchedName.first() && suppliedName.last() == matchedName.last()) {
                match.nameAuthor = (suppliedName - matchedName).join(" ")
                match.fullName = name
            }
        }
        if (!match.fullName) {
            match.fullName = "${match.scientificName} ${match.nameAuthor ?: ""}".trim()
        }

        match
    }

    private populateClassification(Map<String, String> classification, LinnaeanRankClassification rankClassification) {
        classification?.each { rank, rankName ->
            switch (rank?.toLowerCase()) {
                case "kingdom":
                    rankClassification.setKingdom(rankName)
                    break
                case "phylum":
                    rankClassification.setPhylum(rankName)
                    break
                case "klass":
                case "clazz":
                case "class":
                    rankClassification.setKlass(rankName)
                    break
                case "order":
                    rankClassification.setOrder(rankName)
                    break
                case "family":
                    rankClassification.setFamily(rankName)
                    break
                case "genus":
                    rankClassification.setGenus(rankName)
                    break
            }
        }
    }

    Map matchNSLName(String name) {
        Map match = [:]
        try {
            log.debug("GET request to ${grailsApplication.config.nsl.name.match.url.prefix}\"${enc(name)}\"")
            String resp = new URL("${grailsApplication.config.nsl.name.match.url.prefix}\"${enc(name)}\"").text
            def json = new JsonSlurper().parseText(resp)
            if (json.count == 1) {
                match.scientificName = json.names[0].simpleName
                match.scientificNameHtml = json.names[0].simpleNameHtml
                match.fullName = json.names[0].fullName
                match.fullNameHtml = json.names[0].fullNameHtml
                match.nameAuthor = extractAuthorsFromNameHtml(match.fullNameHtml)
                String linkUrl = json.names[0]._links.permalink.link
                match.nslIdentifier = linkUrl.substring(linkUrl.lastIndexOf("/") + 1)
                match.nslProtologue = json.names[0].primaryInstance[0]?.citationHtml
            } else {
                log.warn("${json.count} NSL matches for ${name}")
            }
        } catch (Exception e) {
            log.error(e)
        }

        match
    }

    /**
     * Search the NSL for the name and return a map containing all possible versions of the name (the accepted name,
     * synonyms, etc) with the taxonomic status for each name.
     *
     * @param name The name to search for
     * @return Map containing [alternateName : taxonomicStatus]
     */
    Map<String, String> searchNSLName(String name) {
        Map<String, String> matches = [:]

        String url = "${grailsApplication.config.nsl.search.service.url.prefix}?tree=${NSL_APC_PRODUCT}&q=${enc(StringUtils.capitalize(name))}"
        Map result = webService.get(url)

        if (result?.resp && isSuccessful(result?.statusCode) && result?.resp?.records) {
            // The return structure from the NSL search service is (where all entities are optional)
            // {
            //     records: {
            //         synonyms: [
            //                 …
            //         ],
            //         acceptedNames: {
            //             …,
            //             synonyms: [
            //                     …
            //             ]
            //         }
            //     }
            // }
            result.resp.records.synonyms?.each { Map synonym ->
                matches.put(synonym.canonicalName, synonym.taxonomicStatus)
                if (synonym.acceptedNameUsage) {
                    matches.put(synonym.acceptedNameUsage, synonym.taxonomicStatus)
                }
            }

            result.resp.records.acceptedNames?.each { String nameId, Map nslName ->
                matches.put(nslName.canonicalName, nslName.taxonomicStatus)

                nslName.synonyms?.each { Map synonym ->
                    matches.put(synonym.canonicalName, synonym.taxonomicStatus)
                }
            }
        } else {
            matches = [:]
            log.error("Failed to query NSL for name matches: ${result.error}")
        }

        matches
    }

    String extractAuthorsFromNameHtml(String nameHtml) {
        String names = nameHtml?.
                replaceAll(".*<authors>(.*)</authors>.*", '$1')?.
                replaceAll("</author>", ", ")?.
                replaceAll("<author.*?>", '')?.trim()

        if (names?.endsWith(",")) {
            names = names.substring(0, names.length() - 1)
        }

        names
    }

    Map matchCachedNSLName(Map<String, List> nslCache, String simpleName, String nameAuthor, String fullName) {
        Map match = [:]

        fullName = fullName?.replaceAll(" +", " ")?.trim()
        simpleName = simpleName?.replaceAll(" +", " ")?.trim()

        if (nslCache.byFullName.containsKey(fullName)) {
            match = findBestCachedMatch(nslCache.byFullName[fullName], nameAuthor)
        } else if (nslCache.bySimpleName.containsKey(simpleName)) {
            match = findBestCachedMatch(nslCache.bySimpleName[simpleName], nameAuthor)
        } else {
            log.warn "No match found in NSL name cache for ${fullName}"
        }

        match
    }

    private Map findBestCachedMatch(List<Map> names, String author) {
        Map match = [:]

        // If there is only 1 name:
        //   - if it is valid then use it
        //   - if it is not valid but an author has been provided, then use it
        //   - if it is not valid and no author was provided, then there is no match
        // If there are multiple names:
        //   - if there is only 1 valid name, then use it
        //   - if there are no valid names, then there is no match
        //   - if there are multiple valid names, then there is no match

        if (names.size() == 1 && (names[0].valid || author)) {
            match = names[0]
        } else {
            List validNames = names?.findAll { it.valid }
            if (validNames.size() == 1) {
                match = validNames[0]
            } else if (validNames.size() > 1) {
                List withProtologue = validNames.findAll { it.nslProtologue }
                if (withProtologue.size() == 1) {
                    match = withProtologue[0]
                }
            }
        }

        match
    }

    Map findNslNameFromNomenclature(String nslNomenclatureIdentifier) {
        Map match = [:]

        if (nslNomenclatureIdentifier) {
            String resp = new URL("${grailsApplication.config.nsl.name.instance.url.prefix}${nslNomenclatureIdentifier}.json").text
            def json = new JsonSlurper().parseText(resp)

            String instanceLink = json?.instance?.name?._links?.permalink?.link
            match.nslIdentifier = instanceLink.substring(instanceLink.lastIndexOf("/") + 1)
            match.nslProtologue = json.instance?.reference?.citationHtml
        }

        match
    }

    Map findNomenclature(String nslNameIdentifier, NSLNomenclatureMatchStrategy matchStrategy, List<String> searchText = null) {
        Map match = null;
        switch (matchStrategy) {
            case NSLNomenclatureMatchStrategy.APC_OR_LATEST:
                List concepts = listNomenclatureConcepts(nslNameIdentifier)
                concepts.each {
                    if (it.apcAccepted?.booleanValue()) {
                        match = it
                    }
                }
                if (!match && concepts) {
                    match = concepts.last()
                }
                break
            case NSLNomenclatureMatchStrategy.TEXT_CONTAINS:
                List concepts = listNomenclatureConcepts(nslNameIdentifier)

                match = concepts.find { concept -> searchText.find { text -> concept.name.contains(text) } }
                break
            case NSLNomenclatureMatchStrategy.LATEST:
                List concepts = listNomenclatureConcepts(nslNameIdentifier)

                match = concepts ? concepts.last() : null
                break
            case NSLNomenclatureMatchStrategy.NSL_SEARCH:
                match = findConcept(nslNameIdentifier, searchText[0])
        }

        match
    }

    List listNomenclatureConcepts(String nslNameIdentifier) {
        List concepts = []

        try {
            String resp = new URL("${grailsApplication.config.nsl.service.url.prefix}${nslNameIdentifier}${grailsApplication.config.nsl.service.apni.concept.suffix}").text
            def json = new JsonSlurper().parseText(resp)

            if (json.references) {
                concepts = json.references.collect {
                    String url = it._links.permalink.link
                    String id = url.substring(url.lastIndexOf("/") + 1)
                    [
                            id         : id,
                            url        : url,
                            name       : it.citation,
                            nameHtml   : it.citationHtml,
                            apcAccepted: it.APCReference,
                            citations  : it.citations.collect {
                                [
                                        relationship: it.relationship,
                                        nameType    : it.nameType,
                                        fullName    : it.fullName,
                                        simpleName  : it.simpleName
                                ]
                            }
                    ]
                }
            }
        } catch (Exception e) {
            log.error "Failed to retrieve nomenclature concepts for NSL name id ${nslNameIdentifier}", e
        }

        concepts
    }

    Map findConcept(String nslNameIdentifier, String text) {
        Map concept = [:]

        try {
            if (text) {
                String prefix = grailsApplication.config.nsl.service.url.prefix
                String suffix = grailsApplication.config.nsl.find.concept.service.suffix

                String resp = new URL("${prefix}${nslNameIdentifier}${suffix}${enc(text)}").text

                def json = new JsonSlurper().parseText(resp)

                if (json && json.citation) {
                    concept.name = json.citation
                    concept.nameHtml = json.citationHtml
                    concept.url = json._links.permalink.link
                    concept.id = concept.url.substring(concept.url.lastIndexOf("/") + 1)
                    log.debug("Matched '${text}' to ${concept.id} (${concept.name})")
                } else {
                    log.warn("Could not match concept name '${text}' to anything in the NSL")
                }

            }
        } catch (Exception e) {
            log.error "Failed to find NSL concept", e
        }

        concept
    }

    Map loadNSLSimpleNameDump() {
        log.info "Loading NSL Simple Name dump into memory...."
        long start = System.currentTimeMillis()

        Map result = [bySimpleName: [:].withDefault { [] }, byFullName: [:].withDefault { [] }]

        // Names with these statuses are considered to be 'acceptable' names in the NSL. We do not want to match to any
        // unacceptable names
        List validStatuses = ['legitimate', 'manuscript', 'nom. alt.', 'nom. cons.', 'nom. cons., nom. alt.',
                              'nom. cons., orth. cons.', 'nom. et typ. cons.', 'orth. cons.', 'typ. cons.', '[default]']

        try {
            URL url = new URL("${grailsApplication.config.nsl.name.export.url}")

            url.withReader { reader ->
                def csv = parseCsv(reader)

                csv.each { fields ->
                    Map name = [scientificName    : fields.canonicalName,
                                scientificNameHtml: fields.canonicalNameHTML,
                                fullName          : fields.scientificName,
                                fullNameHtml      : fields.scientificNameHTML,
                                url               : fields.scientificNameID,
                                nslIdentifier     : fields.scientificNameID.substring(fields.scientificNameID.lastIndexOf("/") + 1),
                                rank              : fields.taxonRank,
                                nameAuthor        : fields.scientificNameAuthorship,
                                nslProtologue     : "${fields.namePublishedIn ?: ''} ${fields.namePublishedInYear}".trim(),
                                valid             : validStatuses.contains(fields.nomenclaturalStatus),
                                status            : fields.nomenclaturalStatus]

                    result.bySimpleName[fields.canonicalName] << name
                    result.byFullName[fields.scientificName] << name
                }
            }

        } catch (Exception e) {
            log.error "Failed to load NSL simple name dump", e
        }

        log.info "... finished loading NSL Simple Name dump in ${System.currentTimeMillis() - start} ms"
        result
    }
}
