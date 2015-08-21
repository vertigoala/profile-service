import au.org.ala.profile.marshaller.*

// Place your Spring DSL code here
beans = {

    // required for @Async annotation support
    xmlns task:"http://www.springframework.org/schema/task"
    task.'annotation-driven'('proxy-target-class':true, 'mode':'proxy')

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
