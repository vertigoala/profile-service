package au.org.ala.profile

import static com.xlson.groovycsv.CsvParser.parseCsv
import static au.org.ala.profile.util.Utils.enc

import au.org.ala.profile.util.NSLNomenclatureMatchStrategy
import au.org.ala.profile.util.Utils
import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.model.NameSearchResult
import au.org.ala.names.search.ALANameSearcher
import groovy.json.JsonSlurper
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService extends BaseDataAccessService {

    def grailsApplication
    ALANameSearcher nameSearcher

    @PostConstruct
    def init() {
        nameSearcher = new ALANameSearcher("${grailsApplication.config.name.index.location}")
    }

    Map matchName(String name, String manuallyMatchedGuid = null) {
        LinnaeanRankClassification rankClassification = new LinnaeanRankClassification()
        rankClassification.setScientificName(name)
        NameSearchResult result
        if (manuallyMatchedGuid) {
            result = nameSearcher.searchForRecordByLsid(manuallyMatchedGuid)
        } else {
            result = nameSearcher.searchForAcceptedRecordDefaultHandling(rankClassification, true, true)
        }

        if (result) {
            Map match = [
                    guid          : result.lsid,
                    scientificName: result.getRankClassification().getScientificName(),
                    nameAuthor    : result.getRankClassification().getAuthorship()
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
        } else {
            null
        }
    }

    Map matchNSLName(String name) {
        Map match = [:]
        try {
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

    Map matchCachedNSLName(Map nslCache, String name) {
        Map match = [:]

        if (nslCache.bySimpleName.containsKey(name)) {
            match = nslCache.bySimpleName[name]
        } else if (nslCache.byFullName.containsKey(name)) {
            match = nslCache.byFullName[name]
        } else {
            log.warn "No match found in NSL name cache for ${name}"
        }

        match
    }

    Map findNomenclature(String nslNameIdentifier, NSLNomenclatureMatchStrategy matchStrategy, List<String> searchText = null) {
        Map match = null;
        switch (matchStrategy) {
            case NSLNomenclatureMatchStrategy.APC_OR_LATEST:
                List concepts = listNomenclatureConcepts(nslNameIdentifier)
                concepts.each {
                    if (it.APCReference?.booleanValue()) {
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

                if (json && json.instance) {
                    concept.name = json.instance.citation
                    concept.nameHtml = json.instance.citationHtml
                    concept.url = json.instance._links.permalink.link
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

        Map result = [bySimpleName: [:], byFullName: [:]]

        // Names with these statuses are considered to be 'acceptable' names in the NSL. We do not want to match to any
        // unacceptable names
        List validStatuses = ['legitimate', 'manuscript', 'nom. alt.', 'nom. cons.', 'nom. cons., nom. alt.',
                              'nom. cons., orth. cons.', 'nom. et typ. cons.', 'orth. cons.', 'typ. cons.']

        try {
            URL url = new URL("${grailsApplication.config.nsl.simple.name.export.url}")

            url.withReader { reader ->
                def csv = parseCsv(reader)

                csv.each { fields ->
                    if (validStatuses.contains(fields.nom_stat)) {
                        String simpleName = Utils.cleanupText(fields.simple_name_html)
                        String fullName = Utils.cleanupText(fields.full_name_html)

                        Map name = [scientificName    : simpleName,
                                    scientificNameHtml: fields.simple_name_html,
                                    fullName          : fullName,
                                    fullNameHtml      : fields.full_name_html,
                                    url               : fields.id,
                                    nslIdentifier     : fields.id.substring(fields.id.lastIndexOf("/") + 1),
                                    rank              : fields.rank,
                                    nameAuthor        : fields.authority,
                                    nslProtologue     : fields.proto_citation]

                        result.bySimpleName << [(simpleName): name]
                        result.byFullName << [(fullName): name]
                    }
                }
            }

        } catch (Exception e) {
            log.error "Failed to load NSL simple name dump", e
        }

        log.info "... finished loading NSL Simple Name dump in ${System.currentTimeMillis() - start} ms"
        result
    }
}
