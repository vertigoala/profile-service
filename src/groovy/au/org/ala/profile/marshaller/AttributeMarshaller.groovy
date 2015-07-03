package au.org.ala.profile.marshaller

import au.org.ala.profile.Attribute
import au.org.ala.profile.Opus
import au.org.ala.profile.Profile
import grails.converters.JSON

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4

class AttributeMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Attribute) { Attribute attr ->
            return [
                    uuid     : attr.uuid,
                    title    : attr.title.name,
                    order    : attr.title.order,
                    required : attr.title.required,
                    text     : attr.text,
                    source   : attr.source,
                    plainText: cleanupText(attr.text),
                    creators : attr.creators.collect { it.name },
                    editors  : attr.editors.collect { it.name },
                    original : attr.original,
                    profile  : attr.profile ? marshalProfile(attr.profile) : null
            ]
        }
    }

    def marshalProfile(Profile profile) {
        return [
                uuid          : profile.uuid,
                scientificName: profile.scientificName,
                opus          : marshalOpus(profile.opus)
        ]
    }

    def marshalOpus(Opus opus) {
        return [
                uuid     : opus.uuid,
                title    : opus.title,
                shortName: opus.shortName
        ]
    }

    def cleanupText(str) {
        if (str) {
            str = unescapeHtml4(str)
            // preserve line breaks as new lines, remove all other html
            str = str.replaceAll(/<p\/?>|<br\/?>/, "\n").replaceAll(/<.+?>/, "").trim()
        }
        return str
    }
}