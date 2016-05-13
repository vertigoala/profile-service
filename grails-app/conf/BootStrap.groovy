import au.org.ala.profile.Comment
import au.org.ala.profile.listener.AuditListener
import au.org.ala.profile.listener.LastUpdateListener
import au.org.ala.profile.sanitizer.SanitizedHtml
import au.org.ala.profile.listener.ValueConverterListener
import com.gmongo.GMongo
import com.mongodb.*
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.grails.datastore.mapping.core.Datastore

class BootStrap {

    def auditService
    def authService
    def grailsApplication
    GMongo mongo
    def sanitizerPolicy

    def init = { servletContext ->

        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        initDatastores(ctx)

        ctx.getBean("customObjectMarshallers").register()

        addLastPublishedOnProfiles()
    }
    def destroy = {
    }

    // Add custom GORM event listeners
    def initDatastores(ctx) {
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            log.info "Adding listener for datastore: ${d}"
            ctx.addApplicationListener new AuditListener(d, auditService)
            ctx.addApplicationListener new LastUpdateListener(d, authService)
            ctx.addApplicationListener(ValueConverterListener.of(d, SanitizedHtml, String, sanitizerPolicy.&sanitize))
        }
    }

    // TODO Remove this once all lastPublished fields are added
    def addLastPublishedOnProfiles() {
        log.info("Updating lastPublished field on profiles")
        // Bypass GORM to set the last updated field directly
        DBCollection myColl = mongo.getDB(grailsApplication.config.grails.mongo.databaseName).getCollection("profile")
        BasicDBList list = new BasicDBList()
        list.add(new BasicDBObject('lastPublished', new BasicDBObject('$exists', false)))
        list.add(new BasicDBObject('lastPublished', new BasicDBObject('$type', 10))) // $type: 10 is null
        BasicDBObject condition = new BasicDBObject('$or', list)

        //Find all profiles missing a lastPublished and set it to the lastUpdated
        final cursor = myColl.find(condition)
        final count = cursor.size()
        cursor.each { DBObject profile ->
            profile.lastPublished = profile.lastUpdated
            myColl.save(profile)
        }
        log.info("Updated $count profiles")
    }

}
