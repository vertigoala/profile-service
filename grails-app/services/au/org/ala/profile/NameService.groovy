package au.org.ala.profile

import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.model.NameSearchResult
import au.org.ala.names.search.ALANameSearcher
import groovy.json.JsonSlurper
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService {

    static final String CHAR_ENCODING= "utf-8"

    def grailsApplication
    ALANameSearcher nameSearcher

    @PostConstruct
    def init() {
        nameSearcher = new ALANameSearcher("${grailsApplication.config.name.index.location}")
    }

    Map matchName(String name) {
        LinnaeanRankClassification rankClassification = new LinnaeanRankClassification()
        rankClassification.setScientificName(name)
        NameSearchResult result = nameSearcher.searchForAcceptedRecordDefaultHandling(rankClassification, true, true)

        if (result) {
            Map match = [
                    guid: result.lsid,
                    scientificName: result.getRankClassification().getScientificName(),
                    author: result.getRankClassification().getAuthorship()
            ]

            // Autonym workaround. Autonyms have the author name in the middle, and the name service does not currently
            // (as of 23/6/15) always return the author, even though it may match the name.
            // Therefore, we try to find the author name by comparing the provided name with the matched name, and if
            // they are different, but have the same start and end, then we assume that the difference is the author name.
            // e.g. "Acacia dealbata Link subsp. dealbata" is an autonym, but "Acacia dealbata subsp. subalpina Tindale & Kodela" is a subspecies.
            if (match.author == null && name != match.scientificName) {
                List suppliedName = name.split(" ")
                List matchedName = match.scientificName.split(" ")
                if (suppliedName.first() == matchedName.first() && suppliedName.last() == matchedName.last()) {
                    match.author = (suppliedName - matchedName).join(" ")
                    match.fullName = name
                }
            }
            if (!match.fullName) {
                match.fullName = "${match.scientificName} ${match.author ?: ""}".trim()
            }

            match
        } else {
            null
        }
    }

    def getNSLNameIdentifier(String fullName) {
        String nslIdentifier = null

        try {
            String name = URLEncoder.encode(fullName, CHAR_ENCODING)
            // the NSL service can't handle spaces encoded as +, so have to change them to %20
            name = name.replaceAll("\\+", "%20")
            String resp = new URL("${grailsApplication.config.nsl.name.match.url.prefix}${name}.json").text
            JsonSlurper jsonSlurper = new JsonSlurper()
            def json = jsonSlurper.parseText(resp)
            json.each {
                if (it.startsWith(grailsApplication.config.nsl.name.url.prefix)) {
                    nslIdentifier = it.substring(it.lastIndexOf("/") + 1);
                }
            }
            log.debug "Found NSL ID ${nslIdentifier} for name ${name}"
        } catch (Exception e) {
            log.error e
            null
        }

        nslIdentifier
    }
}
