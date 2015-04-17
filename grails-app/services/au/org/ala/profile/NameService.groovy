package au.org.ala.profile

import org.springframework.transaction.annotation.Transactional
import groovy.json.JsonSlurper

@Transactional
class NameService {

    def grailsApplication

    def getGuidForName(String name) {
        try {
            def resp = new URL("${grailsApplication.config.bie.base.url}/ws/guid/${URLEncoder.encode(name, 'UTF-8')}").text
            def jsonSlurper = new JsonSlurper()
            jsonSlurper.parseText(resp).acceptedIdentifier
        } catch (Exception e) {
            log.error(e.getMessage())
            null
        }
    }
}
