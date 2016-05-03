package au.org.ala.profile

import au.org.ala.profile.util.Utils
import groovy.json.JsonSlurper

class BieService {
    def grailsApplication

    def getClassification(String guid) {
        try {
            String resp = new URL("${grailsApplication.config.bie.base.url}/ws/classification/${guid}").text
            JsonSlurper jsonSlurper = new JsonSlurper()
            jsonSlurper.parseText(resp)
        } catch (Exception e) {
            log.error("Failed to retrieve classification for ${guid}", e)
            null
        }
    }

    def getSpeciesProfile(String guid) {
        try {
            String resp = new URL("${grailsApplication.config.bie.base.url}/ws/species/${guid}").text
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
            log.debug("GET request to ${grailsApplication.config.bie.base.url}/ws/search.json?q=${Utils.enc(name)}")
            String resp = new URL("${grailsApplication.config.bie.base.url}/ws/search.json?q=${Utils.enc(name)}").text

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
            log.debug("GET request to ${grailsApplication.config.bie.base.url}/ws/species/${Utils.enc(name)}")
            String resp = new URL("${grailsApplication.config.bie.base.url}/ws/species/${Utils.enc(name)}").text

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
