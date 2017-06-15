package au.org.ala.profile

import au.org.ala.profile.util.Utils
import com.google.common.collect.Sets
import org.codehaus.groovy.grails.web.util.WebUtils
import org.grails.plugins.metrics.groovy.Metered
import org.grails.plugins.metrics.groovy.Timed

import static au.org.ala.profile.util.Utils.enc

class MasterListService {

    static transactional = false

    static final String MASTER_LIST_OVERRIDE_PARAM = 'florulaOverrideId'

    def grailsApplication
    def webService
    def userService
    def userSettingsService

    /**
     * Return the master list for a given collection or null if no master list is set.
     * @param opus The collection to get the master list for
     * @return A list of objects that probably have name and scientificName properties.
     * @throws MasterListUnavailableException
     */
    @Timed
    @Metered
    List<Map<String, String>> getMasterList(Opus opus) throws MasterListUnavailableException {
        if (!opus.masterListUid) return null
        def listId = opus.masterListUid
        try {
            return getProfileList(listId)
        } catch (e) {
            log.error("Can't get master list for ${opus.shortName}")
            throw e
        }
    }

    private List<Map<String, String>> getProfileList(String listId) {
        def baseUrl = grailsApplication.config.lists.base.url ?: 'https://lists.ala.org.au'
        def response = webService.get("$baseUrl/ws/speciesListItems/${enc(listId)}")
        if (response.statusCode >= 400) {
            log.error("Can't get master list for ${listId}")
            throw new MasterListUnavailableException()
        } else {
            return response.resp?.each { entry ->
                if (entry.name) entry.name = entry.name.trim()
            }
        }
    }

    // TODO replace use of this method with extracting the user and override id from all
    // relevant controller actions and service calls
    @Timed
    @Metered
    List<Map<String,String>> getMasterListForUser(Opus opus) {
        String florulaId
        def userid = userService.currentUserDetails?.userId
        if (!userid) {
            try {
                def wr = WebUtils.retrieveGrailsWebRequest()
                def request = wr.getRequest()
                def query = Utils.parseQueryString(request.queryString)
                florulaId = query['florulaOverrideId']?.first()
            } catch (e) {
                log.error("Not in a request context", e)
                florulaId = null
            }
        } else {
            florulaId = userSettingsService.getUserSettings(userid)?.allFlorulaSettings?.get(opus.uuid)?.drUid
        }
        logUri() // Get an idea which urls this is called from
        return getMasterListForUser(opus, florulaId)
    }

    private void logUri() {
        if (log.isTraceEnabled()) {
            try {
                def request = WebUtils.retrieveGrailsWebRequest().request
                def uri = request.forwardURI
                def method = request.method
                def protocol = request.protocol
                log.trace("!!! GET MASTER LIST FOR USER !!! ($method $uri $protocol)")
            } catch (e) {
                log.trace("!!! GET MASTER LIST FOR USER !!!", new RuntimeException())
            }
        }
    }

    @Timed
    @Metered
    List<Map<String,String>> getMasterListForUser(Opus opus, String florulaId) {
        def florulaListItems = florulaId ? getProfileList(florulaId) : null
        def masterListItems = opus?.masterListUid ?  getProfileList(opus?.masterListUid) : null
        def result
        if (florulaListItems != null && masterListItems != null) {
            def fm = florulaListItems.collectEntries { [(it.name.toLowerCase()): it] }
            def mlm = masterListItems.collectEntries { [(it.name.toLowerCase()): it] }
            def i = Sets.intersection(mlm.keySet(), fm.keySet())
            result = i.collect { fm[it] + mlm[it] } // master list entries win
        } else if (masterListItems != null) {
            result = masterListItems
        } else if (florulaListItems) {
            result = florulaListItems
        } else {
            result = null
        }
        return result
    }

    class MasterListUnavailableException extends IOException {
        @Override synchronized Throwable fillInStackTrace() {
            return this
        }
    }
}
