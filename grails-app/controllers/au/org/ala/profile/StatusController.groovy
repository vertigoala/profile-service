package au.org.ala.profile

import grails.converters.JSON

class StatusController extends BaseController {

    static final String STATUS_OK = "ok"
    static final String STATUS_DEAD = "dead"

    DoiService doiService

    def status() {
        Map health = [:]

        if (!params.component || params.component == "doi") {
            Map doiStatus = doiService.serviceStatus()
            if (doiStatus.statusCode == DoiService.ANDS_RESPONSE_STATUS_OK) {
                health.andsDoiService = [status: STATUS_OK]
            } else {
                health.andsDoiService = [status: STATUS_DEAD, message: "${doiStatus.statusCode} - ${doiStatus.message}"]
            }
        }

        render health as JSON
    }

    def ping() {
        render "server is up!"
    }
}
