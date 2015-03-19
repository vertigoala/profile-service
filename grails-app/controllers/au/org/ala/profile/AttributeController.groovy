package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus

class AttributeController extends BaseController {

    ProfileService profileService

    def index() {
        def attributes = Attribute.findAll([max: 100], {})

        respond attributes, [formats: ['json', 'xml']]
    }

    def show() {
        if (!params.attributeId) {
            badRequest()
        } else {
            def attr = Attribute.findByUuid(params.attributeId)

            respond attr, [formats: ['json', 'xml']]
        }
    }

    def create() {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        Profile profile = Profile.findByUuid(json.profileId)

        if (!profile) {
            notFound()
        } else {
            Attribute attribute = profileService.createAttribute(json.profileId, json)

            if (attribute) {
                response.setStatus(HttpStatus.SC_CREATED)
                def result = [success: true, attributeId: attribute.uuid]
                render result as JSON
            } else {
                saveFailed()
            }
        }
    }

    /**
     * Takes a JSON post supplying a attributeId (uuid), and then a title & text & contributor
     */
    def update() {
        JsonSlurper jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        if (!params.attributeId) {
            badRequest()
        } else {
            boolean success = profileService.updateAttribute(params.attributeId, json)

            if (success) {
                def result = [success: true, attributeId: params.attributeId]
                render result as JSON
            } else {
                notFound()
            }
        }
    }

    def delete() {
        log.debug("Deleting attribute ${params.attributeId} from profile ${params.profileId}")

        boolean success = profileService.deleteAttribute(params.attributeId, params.profileId)

        if (success) {
            response.setStatus(HttpStatus.SC_OK)
            render([success: true] as JSON)
        } else {
            saveFailed()
        }
    }
}
