package au.org.ala.profile

import grails.converters.JSON

class AuditController {

    def index() { }

    def auditTrailForObject(){
        def results = AuditMessage.findAllByEntityId(params.uuid, [max: 10, sort: "date", order: "desc", offset: 0])
        render results as JSON
    }

    def auditTrailForUser(){
        def results = AuditMessage.findAllByUserId(params.userId, [max: 10, sort: "date", order: "desc", offset: 0])
        render results as JSON
    }
}
