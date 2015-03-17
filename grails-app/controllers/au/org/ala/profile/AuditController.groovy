package au.org.ala.profile

import grails.converters.JSON

class AuditController {

    def index() {}

    def auditTrailForObject() {
        log.debug("Finding audit trail for entity ${params.entityId}")
        def results = AuditMessage.findAllByEntityId(params.entityId, [max: 10, sort: "date", order: "desc", offset: 0])
        render results as JSON
    }

    def auditTrailForUser() {
        def results = AuditMessage.findAllByUserId(params.userId, [max: 10, sort: "date", order: "desc", offset: 0])
        render results as JSON
    }
}
