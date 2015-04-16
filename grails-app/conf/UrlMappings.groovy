class UrlMappings {

	static mappings = {

        "/audit/object/$entityId" controller: "audit", action: [GET:"auditTrailForObject"]

        "/audit/user/$userId" controller: "audit", action: [GET:"auditTrailForUser"]

        "/opus/$opusId/glossary/item/" controller:"opus", action: [PUT: "createGlossaryItem"]
        "/opus/$opusId/glossary/item/$glossaryItemId" controller:"opus", action: [DELETE: "deleteGlossaryItem", POST: "updateGlossaryItem"]
        "/opus/$opusId/glossary" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary/$prefix" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary" controller: "opus", action: [POST: "saveGlossaryItems"]

        "/opus/$opusId/vocab/" controller: "vocab", action: "index"
        "/opus/$opusId/vocab/$vocabId" controller: "vocab", action: [GET: "show", POST: "update"]
        "/opus/$opusId/vocab/usages/find" controller: "vocab", action: "findUsagesOfTerm"
        "/opus/$opusId/vocab/usages/replace" controller: "vocab", action: [POST: "replaceUsagesOfTerm"]

        "/opus/" controller: "opus", action: [GET: "index", PUT: "create", POST: "create"]

        "/opus/$opusId/updateUsers" controller: "opus", action: [POST: "updateUsers"]

        "/opus/$opusId" controller: "opus", action: [GET: "show", POST:"updateOpus", DELETE: "deleteOpus"]

        "/profile/search" controller: "profile", action: "search"

        "/profile/" controller: "profile", action: [GET: "index", PUT: "createProfile"]

        "/profile/$profileId" controller: "profile", action: [GET: "getByUuid", DELETE: "deleteProfile", POST: "updateProfile"]

        "/profile/$profileId/attribute/" controller: "attribute", action: [GET:"index", PUT:"create",  POST:"create"]
        "/profile/$profileId/attribute/$attributeId" controller: "attribute", action: [GET:"show", PUT:"update", DELETE:"delete", POST:"update"]

        "/profile/$profileId/links" controller: "profile", action: [POST:"saveLinks"]

        "/profile/$profileId/bhl" controller: "profile", action: [POST:"saveBHLLinks"]
        
        "/profile/$profileId/images" controller: "profile", action: [POST: "saveImages"]
        
        "/profile/$profileId/bibliography" controller: "profile", action: [POST: "saveBibliography"]

        "/profile/$profileId/specimen" controller: "profile", action: [POST: "saveSpecimens"]

        "/profile/$profileId/classification" controller: "profile", action: [GET:"classification"]

        "/profile/$profileId/publication" controller: "profile", action: [GET: "listPublications", POST:"savePublication"]
        "/profile/$profileId/publication/$publicationId/delete" controller: "profile", action: [DELETE:"deletePublication"]
        "/profile/$profileId/publication/$publicationId/file" controller: "profile", action: [GET:"getPublicationFile"]

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/" view:"/index"
        "500" view:'/error'

        // TODO remove these importXYZ and test mappings
        "/importFOA" controller: "profile", action: "importFOA"
        "/importSponges" controller: "profile", action: "importSponges"
        "/createTestOccurrenceSource" controller: 'profile', action: 'createTestOccurrenceSource'

	}
}
