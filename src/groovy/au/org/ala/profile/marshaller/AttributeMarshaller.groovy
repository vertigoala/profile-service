package au.org.ala.profile.marshaller
import au.org.ala.profile.Attribute
import au.org.ala.profile.Opus
import au.org.ala.profile.Profile
import grails.converters.JSON

class AttributeMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Attribute) { Attribute attr ->
            return [
                    uuid: "${attr.uuid}",
                    title: "${attr.title.name}",
                    text: "${attr.text}",
                    creators: attr.creators.collect{ it.name },
                    editors: attr.editors.collect{ it.name },
                    original: attr.original,
                    profile: marshalProfile(attr.profile)
            ]
        }
    }

    def marshalProfile(Profile profile) {
        return [
                uuid: "${profile.uuid}",
                scientificName: "${profile.scientificName}",
                opus: marshalOpus(profile.opus)
        ]
    }

    def marshalOpus(Opus opus) {
        return [
                uuid: "${opus.uuid}",
                title: "${opus.title}"
        ]
    }

}