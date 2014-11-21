package au.org.ala.profile

import grails.converters.JSON
import groovy.json.JsonSlurper

class AttributeController {

    def index() {
        response.setContentType("application/json")
        def attributes = Attribute.findAll([max:100], {})
        if(attributes){
            def toRender = []
            attributes.each { attr ->
                toRender << [
                        "uuid": "${attr.uuid}",
                        "title": "${attr.title}",
                        "text": "${attr.text}",
                        "contributors": attr.contributors.collect{ it.name }
                ]
            }
            response.setContentType("application/json")
            if(params.callback){
                render "${params.callback}(${toRender as JSON})"
            } else {
                render toRender as JSON
            }
        } else {
            response.sendError(400)
        }
    }

    def show(){
        def attr = Attribute.findByUuid(params.uuid)
        response.setContentType("application/json")
        def model = [
            "uuid": "${attr.uuid}",
            "title": "${attr.title}",
            "text": "${attr.text}",
            "contributors": attr.contributors.collect{ it.name }
        ]
        if(params.callback){
            render "${params.callback}(${model as JSON})"
        } else {
            render model as JSON
        }
    }

    def create(){

        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())
        def profile = Profile.findByUuid(json.profileUuid)

        if(profile){

            def attribute = new Attribute(
                    uuid: UUID.randomUUID().toString(),
                    title: json.title,
                    text: json.text
            )
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
            attr.save()
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
//            attr.delete(flush: true)
            response.setStatus(204)
            def result = [success:false]
            render result as JSON
        } else {
            response.sendError(404, "No attribute found for uuid: " + params.uuid)
        }
    }
}
