import au.org.ala.profile.listener.AuditListener
import au.org.ala.profile.listener.LastUpdateListener
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.datastore.mapping.core.Datastore

class BootStrap {

    def auditService
    def authService

    def init = { servletContext ->
        // Add custom GORM event listeners
        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            log.info "Adding listener for datastore: ${d}"
            ctx.addApplicationListener new AuditListener(d, auditService)
            ctx.addApplicationListener new LastUpdateListener(d, authService)
        }

        ctx.getBean("customObjectMarshallers").register()
    }
    def destroy = {
    }
}
