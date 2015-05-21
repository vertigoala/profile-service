class UrlMappings {

	static mappings = {

        "/audit/object/$entityId" controller: "audit", action: [GET:"auditTrailForObject"]

        "/audit/user/$userId" controller: "audit", action: [GET:"auditTrailForUser"]

        "/opus/$opusId/glossary/item/" controller:"opus", action: [PUT: "createGlossaryItem"]
        "/opus/$opusId/glossary/item/$glossaryItemId" controller:"opus", action: [DELETE: "deleteGlossaryItem", POST: "updateGlossaryItem"]
        "/opus/$opusId/glossary" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary/$prefix" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary" controller: "opus", action: [POST: "saveGlossaryItems"]

        "/opus/$opusId/about/" controller: "opus", action: [GET: "about", PUT: "updateAboutHtml"]
        "/opus/$opusId/vocab/" controller: "vocab", action: "index"
        "/opus/$opusId/vocab/$vocabId" controller: "vocab", action: [GET: "show", POST: "update"]
        "/opus/$opusId/vocab/usages/find" controller: "vocab", action: "findUsagesOfTerm"
        "/opus/$opusId/vocab/usages/replace" controller: "vocab", action: [POST: "replaceUsagesOfTerm"]

        "/opus/" controller: "opus", action: [GET: "index", PUT: "create", POST: "create"]

        "/opus/$opusId/updateUsers" controller: "opus", action: [POST: "updateUsers"]

        "/opus/$opusId" controller: "opus", action: [GET: "show", POST:"updateOpus", DELETE: "deleteOpus"]

        "/profile/search" controller: "search", action: "findByScientificName"
        "/profile/search/scientificName" controller: "search", action: "findByScientificName"
        "/profile/search/taxon/name" controller: "search", action: "findByTaxonNameAndLevel"
        "/profile/search/taxon/level" controller: "search", action: "groupByTaxonLevel"
        "/profile/search/taxon/levels" controller: "search", action: "getTaxonLevels"

        "/opus/$opusId/profile/" controller: "profile", action: [GET: "index", PUT: "createProfile"]

        "/opus/$opusId/profile/$profileId" controller: "profile", action: [GET: "getByUuid", DELETE: "deleteProfile", POST: "updateProfile"]

        "/opus/$opusId/profile/$profileId/attribute/" controller: "attribute", action: [GET:"index", PUT:"create",  POST:"create"]
        "/opus/$opusId/profile/$profileId/attribute/$attributeId" controller: "attribute", action: [GET:"show", PUT:"update", DELETE:"delete", POST:"update"]

        "/opus/$opusId/profile/$profileId/links" controller: "profile", action: [POST:"saveLinks"]

        "/opus/$opusId/profile/$profileId/bhl" controller: "profile", action: [POST:"saveBHLLinks"]
        
        "/opus/$opusId/profile/$profileId/images" controller: "profile", action: [POST: "saveImages"]

        "/opus/$opusId/profile/$profileId/authorship" controller: "profile", action: [POST: "saveAuthorship"]

        "/opus/$opusId/profile/$profileId/bibliography" controller: "profile", action: [POST: "saveBibliography"]

        "/opus/$opusId/profile/$profileId/specimen" controller: "profile", action: [POST: "saveSpecimens"]

        "/opus/$opusId/profile/$profileId/classification" controller: "profile", action: [GET:"classification"]

        "/opus/$opusId/profile/$profileId/publication" controller: "profile", action: [GET: "listPublications", POST:"savePublication"]
        "/opus/$opusId/profile/$profileId/publication/$publicationId/delete" controller: "profile", action: [DELETE:"deletePublication"]
        "/opus/$opusId/profile/$profileId/publication/$publicationId/file" controller: "profile", action: [GET:"getPublicationFile"]

        "/opus/$opusId/profile/$profileId/comment" controller: "comment", action: [GET: "getComments", PUT: "addComment"]
        "/opus/$opusId/profile/$profileId/comment/$commentId" controller: "comment", action: [GET: "getComment", POST: "updateComment", DELETE: "deleteComment"]

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
