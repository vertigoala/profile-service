package au.org.ala.profile

import au.org.ala.profile.listener.AuditEventType
import org.bson.types.ObjectId

class AuditMessage {

    ObjectId id
    Date date
    String userId
    String userDisplayName
    AuditEventType eventType
    String entityType
    String entityId
    Map entity

    static constraints = {
        entityId nullable: true
    }

    static mapping = {
        version false
    }
}