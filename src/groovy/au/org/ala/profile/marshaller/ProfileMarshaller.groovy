package au.org.ala.profile.marshaller

import au.org.ala.profile.Profile
import au.org.ala.profile.util.Utils
import grails.converters.JSON

class ProfileMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Profile) { Profile profile ->
            return [
                    uuid                     : profile.uuid,
                    guid                     : profile.guid && profile.guid != "null" ? "${profile.guid}" : "",
                    nslNameIdentifier        : profile.nslNameIdentifier,
                    nslNomenclatureIdentifier: profile.nslNomenclatureIdentifier,
                    nslProtologue            : profile.nslProtologue,
                    dataResourceUid          : profile.opus?.dataResourceUid,
                    opusId                   : profile.opus?.uuid,
                    opusName                 : profile.opus?.title,
                    privateMode              : profile.privateMode,
                    rank                     : profile.rank,
                    scientificName           : profile.scientificName,
                    nameAuthor               : profile.nameAuthor,
                    fullName                 : profile.fullName,
                    matchedName              : profile.matchedName ? [scientificName: profile.matchedName.scientificName,
                                                                      nameAuthor    : profile.matchedName.nameAuthor,
                                                                      fullName      : profile.matchedName.fullName] : null,
                    classification           : profile.classification,
                    attributes               : profile.attributes?.sort(),
                    links                    : profile.links,
                    bhl                      : profile.bhlLinks,
                    primaryImage             : profile.primaryImage,
                    excludedImages           : profile.excludedImages ?: [],
                    stagedImages             : profile.stagedImages ?: null,
                    specimenIds              : profile.specimenIds ?: [],
                    authorship               : profile.authorship?.collect {
                        [category: it.category.name, text: it.text]
                    },
                    bibliography             : profile.bibliography?.collect {
                        [uuid: it.uuid, text: it.text, plainText: Utils.cleanupText(it.text), order: it.order]
                    }?.sort { it.order },
                    publications             : profile.publications?.sort { left, right -> right.publicationDate <=> left.publicationDate },
                    lastAttributeChange      : profile.lastAttributeChange,
                    createdDate              : profile.dateCreated,
                    createdBy                : profile.createdBy,
                    lastUpdated              : profile.lastUpdated,
                    lastUpdatedBy            : profile.lastUpdatedBy,
                    archiveComment           : profile.archiveComment,
                    archivedDate             : profile.archivedDate,
                    archivedBy               : profile.archivedBy,
                    archivedWithName         : profile.archivedWithName
            ]
        }
    }
}
