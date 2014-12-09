package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper

class AttributeController {

    def index() {
        def attributes = Attribute.findAll([max:100], {})
        if(attributes){
            if(params.callback){
                render "${params.callback}(${attributes as JSON})"
            } else {
                respond attributes, [formats:['json', 'xml']]
            }
        } else {
            response.sendError(400)
        }
    }

    def show(){
        def attr = Attribute.findByUuid(params.uuid)
        if(params.callback){
            render "${params.callback}(${attr as JSON})"
        } else {
            respond attr, [formats:['json', 'xml']]
        }
    }

    def create(){

        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def profile = Profile.findByUuid(json.profileUuid)

        def contributor = Contributor.findByUserId(json.userId)
        if(!contributor){
            contributor = new Contributor(userId: json.userId, name: json.userDisplayName)
            contributor.save(flush: true)
        }

        //json.userId
        //json.userDisplayName
        if(profile){

            def attribute = new Attribute(
                    uuid: UUID.randomUUID().toString(),
                    title: json.title,
                    text: json.text
            )
            attribute.creators = [contributor]

            profile.attributes.add(attribute)
            profile.save(flush:true)

            profile.errors.allErrors.each { println( it )}

            //need to handle the attribution
            response.setStatus(201)
            def result = [success:true, uuid: attribute.uuid]
            render result as JSON
        } else {
            response.sendError(404)
            def result = [success:false]
            render result as JSON
        }

    }

    /**
     * Takes a JSON post supplying a UUID, and then a title & text & contributor
     */
    def update(){
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def attr = Attribute.findByUuid(params.uuid)

        if(attr){
            if(json.title) {
                attr.title = json.title
            }
            attr.text = json.text

            def contributor = Contributor.findByUserId(json.userId)
            if(!contributor){
                contributor = new Contributor(userId: json.userId, name: json.userDisplayName)
                contributor.save(flush: true)
            }

            if(!attr.editors){
                attr.editors = []
            }

            if(!attr.editors.contains(contributor)){
                attr.editors << contributor
            }

            attr.save(flush:true)
            //need to handle the attribution
            response.setStatus(201)
            def result = [success:true, uuid: attr.uuid]
            render result as JSON
        } else {
            response.sendError(404)
            def result = [success:false]
            render result as JSON
        }
    }

    def delete(){

        def attr = Attribute.findByUuid(params.uuid)
        def profile = Profile.findByUuid(params.profileUuid)

        if(attr && profile){
            //remove from profile
            profile.attributes.remove(attr)
            profile.save(flush:true)
        }

        if(attr){
            response.setStatus(204)
            def result = [success:false]
            render result as JSON
        } else {
            response.sendError(404, "No attribute found for uuid: " + params.uuid)
        }
    }
}
