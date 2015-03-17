package au.org.ala.profile

import grails.converters.JSON

import static org.apache.http.HttpStatus.*

class BaseController {
    public static final String CONTEXT_TYPE_JSON = "application/json"

    def notFound() {
        sendError(SC_NOT_FOUND)
    }

    def badRequest() {
        sendError(SC_BAD_REQUEST)
    }

    def success(resp) {
        response.status = SC_OK
        response.setContentType(CONTEXT_TYPE_JSON)
        render resp as JSON
    }

    def saveFailed() {
        sendError(SC_INTERNAL_SERVER_ERROR)
    }

    def sendError = {int status, String msg = null ->
        response.status = status
        response.sendError(status, msg)
    }
}
