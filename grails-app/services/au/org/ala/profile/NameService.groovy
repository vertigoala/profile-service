package au.org.ala.profile

import grails.transaction.Transactional
import groovy.json.JsonSlurper

@Transactional
class NameService {

    def getGuidForName(String name) {
        try {
            def resp = new URL("http://bie.ala.org.au/ws/guid/" + URLEncoder.encode(name, "UTF-8")).text
            def jsonSlurper = new JsonSlurper()
            jsonSlurper.parseText(resp).acceptedIdentifier
        } catch (Exception e) {
            log.error(e.getMessage())
            null
        }
    }
}
