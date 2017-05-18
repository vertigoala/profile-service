package au.org.ala.profile

import org.grails.plugins.metrics.groovy.Metered
import org.grails.plugins.metrics.groovy.Timed

class MasterListService {

    static transactional = false

    def grailsApplication
    def webService

    /**
     * Return the master list for a given collection or null if no master list is set.
     * @param opus The collection to get the master list for
     * @return A list of objects that probably have name and scientificName properties.
     * @throws MasterListUnavailableException
     */
    @Timed
    @Metered
    List<Map<String, String>> getMasterList(Opus opus) throws MasterListUnavailableException {
        def baseUrl = grailsApplication.config.lists.base.url ?: 'https://lists.ala.org.au'
        if (!opus.masterListUid) return null
        def response = webService.get("$baseUrl/ws/speciesListItems/${opus.masterListUid}" )
        if (response.statusCode >= 400) {
            log.error("Can't get master list for ${opus.shortName}")
            throw new MasterListUnavailableException()
        } else {
            return response.resp
        }
    }

    class MasterListUnavailableException extends IOException {
        @Override synchronized Throwable fillInStackTrace() {
            return this
        }
    }
}
