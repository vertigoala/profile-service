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
            log.error(e.getMessage())
            null
        }
    }
}
