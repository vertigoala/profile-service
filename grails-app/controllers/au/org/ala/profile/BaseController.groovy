package au.org.ala.profile

import static au.org.ala.profile.util.Utils.isUuid
import grails.converters.JSON

import static org.apache.http.HttpStatus.*

class BaseController {
    public static final String CONTEXT_TYPE_JSON = "application/json"

    ProfileService profileService

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

            // names can be changed, so if there is no profile with the name, check for a draft with that name, but only if the 'latest' flag is true
            if (!profile && params.latest?.toBoolean()) {
                List matches = Profile.withCriteria {
                    eq "opus", opus
                    ilike "draft.scientificName", params.profileId
                }
                profile = matches.isEmpty() ? null : matches.first()
            }
        }

        if (profile && profile.classification) {
            def classifications = profile.draft && params.latest == "true" ? profile.draft.classification : profile.classification
            classifications.each { cl ->
                cl.childCount = Profile.withCriteria {
                    eq "opus", profile.opus
                    isNull "archivedDate"
                    ne "uuid", profile.uuid

                    "classification" {
                        eq "rank", "${cl.rank.toLowerCase()}"
                        ilike "name", "${cl.name}"
                    }

                    projections {
                        count()
                    }
                }[0]

                Profile relatedProfile = Profile.findByScientificNameAndGuidAndOpusAndArchivedDateIsNull(cl.name, cl.guid, opus)
                if (!relatedProfile) {
                    relatedProfile = Profile.findByGuidAndOpusAndArchivedDateIsNull(cl.guid, opus)
                }
                cl.profileId = relatedProfile?.uuid
                cl.profileName = relatedProfile?.scientificName
            }
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
