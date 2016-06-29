package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON

import javax.validation.constraints.NotNull

@RequireApiKey
class AdminController extends BaseController {
    AdminService adminService

    def rematchNames() {
        List opusIds = request.getJSON()?.opusIds ?: []

        NameRematch result = adminService.rematchAllNames(opusIds)

        render result as JSON
    }

    def getTag(String tagId) {
        if (tagId) {
            Tag tag = Tag.findByUuid(tagId)

            if (tag) {
                render tag as JSON
            } else {
                notFound "No matching tag was found for id ${tagId}"
            }
        } else {
            List<Tag> tags = Tag.list()

            render tags as JSON
        }
    }

    def createTag() {
        Map json = request.getJSON()
        if (!json) {
            badRequest "A json body is required"
        } else {
            Tag tag = adminService.createTag(json)

            render tag as JSON
        }
    }

    def deleteTag(@NotNull String tagId) {
        adminService.deleteTag(tagId)

        success [:]
    }

    def updateTag(@NotNull String tagId) {
        Map json = request.getJSON()
        if (!json) {
            badRequest "A json body is required"
        } else {
            Tag tag = adminService.updateTag(tagId, json)

            render tag as JSON
        }
    }
}
