package au.org.ala.profile.marshaller

import au.org.ala.profile.Attribute
import au.org.ala.profile.AuditMessage
import grails.converters.JSON

public class AuditMessageMarshaller {

    void register() {
        JSON.registerObjectMarshaller(AuditMessage) { AuditMessage auditMessage ->

            def object = null

            if(auditMessage.entityType == Attribute.class.getName()){
                object = new Attribute(auditMessage.entity)
            }
            return [
                    uuid : auditMessage.entityId,
                    userId : auditMessage.userId,
                    userDisplayName : auditMessage.userDisplayName,
                    object: object
            ]
        }
    }

}