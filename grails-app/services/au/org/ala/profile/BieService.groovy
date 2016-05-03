package au.org.ala.profile

import au.org.ala.profile.util.Utils
import groovy.json.JsonSlurper

class BieService {
    def grailsApplication

    def getClassification(String guid) {
        try {
            String resp = new URL("${grailsApplication.config.bie.base.url}/classification/${guid}").text
            JsonSlurper jsonSlurper = new JsonSlurper()
            jsonSlurper.parseText(resp)
        } catch (Exception e) {
            log.error("Failed to retrieve classification for ${guid}", e)
            null
        }
    }

    def getSpeciesProfile(String guid) {
        try {
            String resp = new URL("${grailsApplication.config.bie.base.url}/species/${guid}").text
            JsonSlurper jsonSlurper = new JsonSlurper()
            jsonSlurper.parseText(resp)
        } catch (Exception e) {
            log.error("Failed to retrieve species profile for ${guid}", e)
            null
        }
    }

    Set<String> searchForPossibleMatches(String name) {
        Set potentialMatches = [] as HashSet

        try {
            String url = "${grailsApplication.config.bie.base.url}/search.json?q=${Utils.enc(name)}&q.op=AND"
            log.debug("GET request to ${url}")
            String resp = new URL(url).text

            if (resp) {
                Map json = new JsonSlurper().parseText(resp)

                json?.searchResults?.results?.each {
                    String result = it.acceptedConceptName?.trim()
                    if (result && !potentialMatches.find { it.equalsIgnoreCase(result) }) {
                        potentialMatches << result
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to find potential matches for ${name}", e)
        }

        log.debug("Potential name matches from the BIE for ${name} are: ${potentialMatches}")

        potentialMatches
    }

    Set<String> getOtherNames(String name) {
        Set otherNames = [] as HashSet

        try {
            String url = "${grailsApplication.config.bie.base.url}/species/${Utils.enc(name)}"
            log.debug("GET request to ${url}")
            String resp = new URL(url).text

            if (resp) {
                Map json = new JsonSlurper().parseText(resp)

                json.synonyms?.each {
                    otherNames << it.nameString?.trim()
                }

                json.commonNames?.each {
                    otherNames << it.nameString.trim()
                }
            }
        } catch (Exception e) {
            log.error("Failed to find other names for ${name}", e)
        }

        otherNames
    }
}
