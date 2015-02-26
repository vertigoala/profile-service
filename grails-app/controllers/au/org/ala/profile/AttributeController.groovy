package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus

class AttributeController {

    ProfileService profileService

    def index() {
        def attributes = Attribute.findAll([max: 100], {})
        if (attributes) {
            if (params.callback) {
                render "${params.callback}(${attributes as JSON})"
            } else {
                respond attributes, [formats: ['json', 'xml']]
            }
        } else {
            response.sendError(400)
        }
    }

    def show() {
        def attr = Attribute.findByUuid(params.attributeId)
        if (params.callback) {
            render "${params.callback}(${attr as JSON})"
        } else {
            respond attr, [formats: ['json', 'xml']]
        }
    }

    def create() {

        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def profile = Profile.findByUuid(json.profileId)

        def contributor = Contributor.findByUserId(json.userId)
        if (!contributor) {
            contributor = new Contributor(userId: json.userId, name: json.userDisplayName)
            contributor.save(flush: true)
        }

        //json.userId
        //json.userDisplayName
        if (profile) {

            def attribute = new Attribute(
                    uuid: UUID.randomUUID().toString(),
                    title: json.title,
                    text: json.text
            )
            attribute.creators = [contributor]

            profile.attributes.add(attribute)
            profile.save(flush: true)

            profile.errors.allErrors.each { println(it) }

            //need to handle the attribution
            response.setStatus(201)
            def result = [success: true, attributeId: attribute.uuid]
            render result as JSON
        } else {
            response.sendError(404)
            def result = [success: false]
            render result as JSON
        }

    }

    /**
     * Takes a JSON post supplying a attributeId (uuid), and then a title & text & contributor
     */
    def update() {
        JsonSlurper jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        Attribute attr = Attribute.findByUuid(params.attributeId)

        if (attr) {
            if (json.title) {
                attr.title = json.title
            }
            attr.text = json.text

            def contributor = Contributor.findByUserId(json.userId)
            if (!contributor) {
                contributor = new Contributor(userId: json.userId, name: json.userDisplayName)
                contributor.save(flush: true)
            }

            if (!attr.editors) {
                attr.editors = []
            }

            if (!attr.editors.contains(contributor)) {
                attr.editors << contributor
            }

            attr.save(flush: true)
            //need to handle the attribution
            response.setStatus(201)
            def result = [success: true, attributeId: attr.uuid]
            render result as JSON
        } else {
            response.sendError(404)
            def result = [success: false]
            render result as JSON
        }
    }

    def delete() {
        log.debug("Deleting attribute ${params.attributeId} from profile ${params.profileId}")

        boolean success = profileService.deleteAttribute(params.attributeId, params.profileId)

        if (success) {
            response.setStatus(HttpStatus.SC_OK)
            def result = [success: true]
            render result as JSON
        } else {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed to delete attribute")
        }
    }
}
