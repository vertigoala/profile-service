package au.org.ala.profile

import grails.converters.JSON

import static org.apache.http.HttpStatus.*

class BaseController {
    public static final String CONTEXT_TYPE_JSON = "application/json"
    public static final String UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

    boolean isUuid(String str) {
        str =~ UUID_REGEX
    }

    def notFound = {String message = null ->
        sendError(SC_NOT_FOUND, message ?: "")
    }

    def badRequest = {String message = null ->
        sendError(SC_BAD_REQUEST, message ?: "")
    }

    def success = { resp ->
        response.status = SC_OK
        response.setContentType(CONTEXT_TYPE_JSON)
        render resp as JSON
    }

    def saveFailed = {
        sendError(SC_INTERNAL_SERVER_ERROR)
    }

    def sendError = {int status, String msg = null ->
        response.status = status
        response.sendError(status, msg)
    }

    Profile getProfile() {
        Profile profile
        if (isUuid(params.profileId)) {
            profile = Profile.findByUuid(params.profileId)
        } else {
            Opus opus = getOpus()
            profile = Profile.findByOpusAndScientificNameIlike(opus, params.profileId)
        }
        profile
    }

    Opus getOpus() {
        Opus opus
        if (isUuid(params.opusId)) {
            opus = Opus.findByUuid(params.opusId)
        } else {
            opus = Opus.findByShortName(params.opusId.toLowerCase())
        }
        opus
    }
}
