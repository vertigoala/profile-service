package au.org.ala.profile

import org.apache.http.HttpStatus

class AccessTokenFilters {

    def grailsApplication
    static final String ACCESS_TOKEN_HEADER = 'ACCESS-TOKEN'

    def filters = {

        apiKeyCheck(controller: '*', action: '*') {
            before = {
                def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
                Class controllerClass = controller?.clazz

                if (controllerClass && controllerClass.package.name.startsWith("au.org.ala.profile.api")) {
                    String token = request.getHeader(ACCESS_TOKEN_HEADER)
                    String opusId = params.opusId

                    if (!token || !opusId || Opus.findByUuid(opusId)?.accessToken != token) {
                        log.warn("No valid access token for opus ${opusId} when calling ${controllerName}/${actionName}")
                        response.status = HttpStatus.SC_FORBIDDEN
                        response.sendError(HttpStatus.SC_FORBIDDEN, "Forbidden")
                        return false
                    }
                }
            }
            after = { Map model ->
            }
            afterView = { Exception e ->
            }
        }
    }
}
