import au.org.ala.profile.Profile
import au.org.ala.profile.Tag
import au.org.ala.profile.listener.AuditListener
import au.org.ala.profile.listener.LastUpdateListener
import au.org.ala.profile.sanitizer.SanitizedHtml
import au.org.ala.profile.listener.ValueConverterListener
import com.mongodb.*
import grails.core.ApplicationAttributes
import org.grails.datastore.mapping.core.Datastore

class BootStrap {

    def auditService
    def authService
    def grailsApplication
    Mongo mongo
    def sanitizerPolicy

    def init = { servletContext ->

        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)

        ctx.getBean("customObjectMarshallers").register()

        createDefaultTags()

        fixMultimedia()

        addStatusToProfiles()

        addTimestampToOpera()

        addScientificNameLowerToProfiles()
    }
    def destroy = {
    }

    void createDefaultTags() {
        if (Tag.count() == 0) {
            Tag iek = new Tag(uuid: UUID.randomUUID().toString(), abbrev: "IEK", name: "Indigenous Ecological Knowledge", colour: "#c7311c")
            iek.save(flush: true)

            Tag flora = new Tag(uuid: UUID.randomUUID().toString(), abbrev: "FLORA", name: "Flora Treatments", colour: "#2ac71c")
            flora.save(flush: true)

            Tag fauna = new Tag(uuid: UUID.randomUUID().toString(), abbrev: "FAUNA", name: "Fauna Treatments", colour: "#8d968c")
            fauna.save(flush: true)
        }
    }

}
