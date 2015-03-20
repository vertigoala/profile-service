package au.org.ala.profile.marshaller

import au.org.ala.profile.Profile
import grails.converters.JSON

class ProfileMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Profile) { Profile profile ->
            return [
                "uuid" : "${profile.uuid}",
                "guid" : profile.guid && profile.guid != "null" ? "${profile.guid}" : "",
                "dataResourceUid" : "${profile.opus.dataResourceUid}",
                "opusId" : "${profile.opus.uuid}",
                "opusName" : "${profile.opus.title}",
                "scientificName" : "${profile.scientificName}",
                "attributes": profile.attributes?.sort { it.title.name.toLowerCase() },
                "links":profile.links,
                "bhl":profile.bhlLinks
            ]
        }
    }
}
