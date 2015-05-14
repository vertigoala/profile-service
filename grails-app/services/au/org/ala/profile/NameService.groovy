package au.org.ala.profile

import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.search.ALANameSearcher
import groovy.json.JsonSlurper
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService {

    static final String CHAR_ENCODING= "utf-8"

    def grailsApplication
    ALANameSearcher nameSearcher
    BieService bieService

    @PostConstruct
    def init() {
        nameSearcher = new ALANameSearcher("${grailsApplication.config.name.index.location}")
    }

    def getGuidForName(String name) {
        LinnaeanRankClassification rankClassification = new LinnaeanRankClassification()
        rankClassification.setScientificName(name)
        nameSearcher.searchForAcceptedLsidDefaultHandling(rankClassification, true, true)
    }

    def getNSLNameIdentifier(String guid) {
        String nslIdentifier = null

        try {
            def speciesProfile = bieService.getSpeciesProfile(guid)

            if (speciesProfile) {
                String name = URLEncoder.encode("${speciesProfile.taxonConcept.nameString} ${speciesProfile.taxonConcept.author}", CHAR_ENCODING)
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
            } else {
                log.debug "No matching species profile found for guid ${guid}"
            }
        } catch (Exception e) {
            log.error e
            null
        }

        nslIdentifier
    }
}
