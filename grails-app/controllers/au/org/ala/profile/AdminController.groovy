package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON

@RequireApiKey
class AdminController extends BaseController {
    AdminService adminService

    def rematchNames() {
        List opusIds = request.getJSON()?.opusIds ?: []

        NameRematch result = adminService.rematchAllNames(opusIds)

        render result as JSON
    }
}
