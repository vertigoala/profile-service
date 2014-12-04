package au.org.ala.profile

import grails.converters.JSON

class AuditController {

    def index() { }

    def auditTrailForObject(){
        def results = AuditMessage.findAllByEntityId(params.uuid)
        render results as JSON
    }

    def auditTrailForUser(){
        def results = AuditMessage.findAllByUserId(params.userId)
        render results as JSON
    }
}
