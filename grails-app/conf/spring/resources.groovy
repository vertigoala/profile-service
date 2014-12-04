import au.org.ala.profile.marshaller.AttributeMarshaller
import au.org.ala.profile.marshaller.AuditMessageMarshaller
import au.org.ala.profile.marshaller.CustomObjectMarshallers

// Place your Spring DSL code here
beans = {

    customObjectMarshallers( CustomObjectMarshallers ) {
        marshallers = [
            new AuditMessageMarshaller(),
            new AttributeMarshaller()
        ]
    }
}
