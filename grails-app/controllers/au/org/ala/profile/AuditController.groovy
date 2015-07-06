package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON

@RequireApiKey
class AuditController {

    def index() {}

    def auditTrailForObject() {
        log.debug("Finding audit trail for entity ${params.entityId}")
        def results = AuditMessage.findAllByEntityId(params.entityId, [sort: "date", order: "desc", offset: 0])
        render results as JSON
    }

    def auditTrailForUser() {
        def results = AuditMessage.findAllByUserId(params.userId, [sort: "date", order: "desc", offset: 0])
        render results as JSON
    }
}
