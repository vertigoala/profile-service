package au.org.ala.profile

import grails.test.spock.IntegrationSpec

class BaseIntegrationSpec extends IntegrationSpec {

    def grailsApplication

    /**
     * MongoDB does not support transactions, so any data changed in test will not be rolled back by grails.
     * Therefore, before each test, loop through all domain classes and drop the mongo collection.
     */
    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def save(entity) {
        entity.save(flush: true, failOnError: true)
    }
}
