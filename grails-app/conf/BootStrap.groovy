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

        addLastUpdatedOnComments()
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

    // TODO Remove this once all lastUpdated fields are added
    def addLastUpdatedOnComments() {
        log.info("Updating lastUpdated field on comments")
        // Bypass GORM to set the last updated field directly
        DBCollection myColl = mongo.getDB(grailsApplication.config.grails.mongo.databaseName).getCollection("comment")
        BasicDBList list = new BasicDBList()
        list.add(new BasicDBObject('lastUpdated', new BasicDBObject('$exists', false)))
        list.add(new BasicDBObject('lastUpdated', new BasicDBObject('$type', 10))) // $type: 10 is null
        BasicDBObject condition = new BasicDBObject('$or', list)

        //Find all docs missing a lastUpdated and set it to the dateCreated
        final cursor = myColl.find(condition)
        final count = cursor.size()
        cursor.each { DBObject comment ->
            comment.lastUpdated = comment.dateCreated
            myColl.save(comment)
        }
        log.info("Updated $count comments")
    }

}
