package au.org.ala.profile

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

    Set<String> getOtherNames(String name) {
        Set otherNames = [] as HashSet

        try {
            String resp = new URL("${grailsApplication.config.bie.base.url}/ws/species/${name}").text
            Map json = new JsonSlurper().parseText(resp)

            json.synonyms?.each {
                otherNames << it.nameString?.trim()
            }

            json.commonNames?.each {
                otherNames << it.nameString.trim()
            }
        } catch (Exception e) {
            log.error("Failed to find other names for ${name}", e)
        }


        otherNames
    }
}
