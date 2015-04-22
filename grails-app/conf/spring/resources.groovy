import au.org.ala.profile.marshaller.*

// Place your Spring DSL code here
beans = {

    customObjectMarshallers( CustomObjectMarshallers ) {
        marshallers = [
            new AuditMessageMarshaller(),
            new AttributeMarshaller(),
            new OpusMarshaller(),
            new ProfileMarshaller(),
            new PublicationMarshaller(),
            new GlossaryMarshaller(),
            new CommentMarshaller()
        ]
    }
}
