package au.org.ala.profile

import static au.org.ala.profile.util.Utils.enc

import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.model.NameSearchResult
import au.org.ala.names.search.ALANameSearcher
import groovy.json.JsonSlurper
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService {

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

    Map matchNSLId(Integer id) {
        Map match = [:]
        try {
            String resp = new URL("${grailsApplication.config.nsl.name.instance.url.prefix}\"${id}\"").text
            def json = new JsonSlurper().parseText(resp)
            if (json.instance) {
                match.scientificName = json.names[0].name.simpleName
                match.fullName = json.names[0].name.fullName
                match.nameAuthor = json.names[0].name.author.name
                String linkUrl = json.names[0].name._links.permalinks.find {
                    it.preferred == "true" || it.preferred == true
                }?.link
                match.nslIdentifier = linkUrl.substring(linkUrl.lastIndexOf("/") + 1)
                match.nslProtologue = json.names[0].name?.primaryInstance[0]?.citationHtml
            } else {
                log.warn("${json.count} NSL matches for ${name}")
            }
        } catch (Exception e) {
            log.error e
        }

        match
    }

    Map matchNSLName(String name) {
        Map match = [:]
        try {
            String resp = new URL("${grailsApplication.config.nsl.name.match.url.prefix}\"${enc(name)}\"").text
            def json = new JsonSlurper().parseText(resp)
            if (json.count == 1) {
                match.scientificName = json.names[0].name.simpleName
                match.fullName = json.names[0].name.fullName
                match.nameAuthor = json.names[0].name.author.name
                String linkUrl = json.names[0].name._links.permalinks.find {
                    it.preferred == "true" || it.preferred == true
                }?.link
                match.nslIdentifier = linkUrl.substring(linkUrl.lastIndexOf("/") + 1)
                match.nslProtologue = json.names[0].name?.primaryInstance[0]?.citationHtml
            } else {
                log.warn("${json.count} NSL matches for ${name}")
            }
        } catch (Exception e) {
            log.error e
        }

        match
    }

}
