package au.org.ala.profile

class FlushAuditMessageQueueJob {

    def auditService

    static triggers = {
      simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {

        // This method has internal session management so doesn't need to be wrapped like the call to the
        // elasticSearchService below.
        auditService.flushMessageQueue()
    }
}
