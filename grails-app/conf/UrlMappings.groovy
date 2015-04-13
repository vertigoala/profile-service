class UrlMappings {

	static mappings = {


        "/audit/object/$entityId"(controller: "audit", action: [GET:"auditTrailForObject"])

        "/audit/user/$userId"(controller: "audit", action: [GET:"auditTrailForUser"])
        
        "/classification"(controller: "profile", action: [GET:"classification"])

        "/glossary/$opusId/item/"(controller:"opus", action: [PUT: "createGlossaryItem"])
        "/glossary/$opusId/item/$glossaryItemId"(controller:"opus", action: [DELETE: "deleteGlossaryItem", POST: "updateGlossaryItem"])
        "/glossary/$opusId"(controller: "opus", action: [GET: "getGlossary"])
        "/glossary/$opusId/$prefix"(controller: "opus", action: [GET: "getGlossary"])
        "/glossary"(controller: "opus", action: [POST: "saveGlossaryItems"])

        "/vocab/"(controller: "vocab", action: "index")

        "/vocab/$vocabId"(controller: "vocab", action: [GET: "show", POST: "update"])

        "/vocab/usages/find"(controller: "vocab", action: "findUsagesOfTerm")

        "/vocab/usages/replace"(controller: "vocab", action: [POST: "replaceUsagesOfTerm"])

        "/attribute/"(controller: "attribute", action: [GET:"index", PUT:"create",  POST:"create"])

        "/attribute/$attributeId"(controller: "attribute", action: [GET:"show", PUT:"update", DELETE:"delete", POST:"update"])

        "/opus/"(controller: "opus", action: [GET: "index", PUT: "create", POST: "create"])

        "/opus/$opusId/updateUsers"(controller: "opus", action: [POST: "updateUsers"])

        "/opus/$opusId"(controller: "opus", action: [GET: "show", POST:"updateOpus", DELETE: "deleteOpus"])

        "/opus/taxaUpload"(controller: "opus", action: "taxaUpload")

        "/profile/search"(controller: "profile", action: "search")

        "/profile/$profileId"(controller: "profile", action: [GET: "getByUuid", DELETE: "deleteProfile", POST: "updateProfile"])

        "/profile/"(controller: "profile", action: [GET: "index", PUT: "createProfile"])

        "/profile/links/$profileId"(controller: "profile", action: [POST:"saveLinks"])

        "/profile/bhl/$profileId"(controller: "profile", action: [POST:"saveBHLLinks"])

        "/profile/publication/$profileId"(controller: "profile", action: [GET: "listPublications", POST:"savePublication"])
        "/profile/publication/$publicationId/delete"(controller: "profile", action: [DELETE:"deletePublication"])
        "/profile/publication/$publicationId/file"(controller: "profile", action: [GET:"getPublicationFile"])

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')

        // TODO remove these importXYZ and test mappings
        "/importFOA"(controller: "profile", action: "importFOA")
        "/importSponges"(controller: "profile", action: "importSponges")
        "/createTestOccurrenceSource"(controller: 'profile', action: 'createTestOccurrenceSource')

	}
}
