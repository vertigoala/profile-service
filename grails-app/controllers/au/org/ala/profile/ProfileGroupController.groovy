package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import com.google.common.base.Stopwatch
import grails.converters.JSON

import static au.org.ala.profile.util.Utils.isUuid

@RequireApiKey
/**
 * Grails controller for profile group actions
 */
class ProfileGroupController extends BaseController {

    ProfileGroupService profileGroupService

    def index() {
        def groups = ProfileGroup.findAll()
        respond groups
    }

    def show() {
        ProfileGroup group = getGroup()
        if (group) {
            int profiles = Profile.countByGroup(group)
            group.profileCount = profiles
            render group as JSON
        } else {
            notFound()
        }
    }

    def createProfileGroup() {
        def json = request.getJSON()

        if (!json || !json.language || !json.opusId) {
            badRequest "A json body with at least the language and an opus id is required"
        } else {
            Opus opus = getOpus()
            if (!opus) {
                notFound "No matching opus can be found"
            } else {
                ProfileGroup group = ProfileGroup.findByLanguageAndOpus(json.language, opus)

                if (group) {
                    badRequest "A profile group already exists for ${json.language}"
                } else {
                    group = profileGroupService.createGroup(opus.uuid, json);
                    render group as JSON
                }
            }
        }
    }

    def deleteProfileGroup() {
        if (!params.groupId) {
            badRequest "You must provide an groupId"
        } else {
            ProfileGroup group = getGroup()

            if (!group) {
                notFound()
            } else {
                boolean success = profileGroupService.deleteGroup(group.uuid)

                render([success: success] as JSON)
            }
        }
    }

    def updateProfileGroup() {
        if (!params.groupId) {
            badRequest "You must provide an groupId"
        } else {
            ProfileGroup group = getGroup()

            if (!group) {
                notFound()
            } else {
                def newValues = request.getJSON()
                boolean updated = profileGroupService.updateGroup(group.uuid, newValues)

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    private ProfileGroup getGroup() {
        ProfileGroup group = ProfileGroup.findByUuid(params.groupId)
        group
    }

}
