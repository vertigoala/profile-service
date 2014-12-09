package au.org.ala.profile.marshaller

import au.org.ala.profile.Link
import grails.converters.JSON

class LinkMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Link) { Link link ->
            return [
                "uuid":"${it.uuid}",
                "url":"${it.url}",
                "title":"${it.title}",
                "fullTitle":"${it.fullTitle}",
                "edition":"${it.edition}",
                "publisherName":"${it.publisherName}",
                "doi":"${it.doi}",
                "description": "${it.description}",
                "creators": it.creators.collect { it.name }
            ]
        }
    }

}
