package au.org.ala.profile.marshaller
import au.org.ala.profile.Attribute
import grails.converters.JSON

class AttributeMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Attribute) { Attribute attr ->
            return [
                    uuid: "${attr.uuid}",
                    title: "${attr.title}",
                    text: "${attr.text}",
                    creators: attr.creators.collect{ it.name },
                    editors: attr.editors.collect{ it.name }
            ]
        }
    }

}