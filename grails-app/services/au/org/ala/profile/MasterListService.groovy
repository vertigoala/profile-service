package au.org.ala.profile

class MasterListService {

    static transactional = false

    def grailsApplication
    def webService

    List<Map> getMasterList(Opus opus) throws MasterListUnavailableException {
        def baseUrl = grailsApplication.config.lists.base.url ?: 'https://lists.ala.org.au'
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
