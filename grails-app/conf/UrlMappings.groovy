class UrlMappings {

    static mappings = {

        "/audit/object/$entityId" controller: "audit", action: [GET: "auditTrailForObject"]

        "/audit/user/$userId" controller: "audit", action: [GET: "auditTrailForUser"]

        "/opus/$opusId/glossary/item/" controller: "opus", action: [PUT: "createGlossaryItem"]
        "/opus/$opusId/glossary/item/$glossaryItemId" controller: "opus", action: [DELETE: "deleteGlossaryItem", POST: "updateGlossaryItem"]
        "/opus/$opusId/glossary" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary/$prefix" controller: "opus", action: [GET: "getGlossary"]
        "/opus/$opusId/glossary" controller: "opus", action: [POST: "saveGlossaryItems"]

        "/opus/$opusId/about/" controller: "opus", action: [GET: "about", PUT: "updateAbout"]
        "/opus/$opusId/vocab/" controller: "vocab", action: "index"
        "/opus/$opusId/vocab/$vocabId" controller: "vocab", action: [GET: "show", POST: "update"]
        "/opus/$opusId/vocab/usages/find" controller: "vocab", action: "findUsagesOfTerm"
        "/opus/$opusId/vocab/usages/replace" controller: "vocab", action: [POST: "replaceUsagesOfTerm"]

        "/opus/" controller: "opus", action: [GET: "index", PUT: "create", POST: "create"]

        "/opus/$opusId/updateUsers" controller: "opus", action: [POST: "updateUsers"]

        "/opus/$opusId/supportingCollections/respond/$requestingOpusId/$requestAction" controller: "opus", action: [POST: "respondToSupportingOpusRequest"]
        "/opus/$opusId/supportingCollections/update" controller: "opus", action: [POST: "updateSupportingOpuses"]

        "/opus/$opusId" controller: "opus", action: [GET: "show", POST: "updateOpus", DELETE: "deleteOpus"]

        "/profile/search" controller: "search", action: "findByScientificName"
        "/profile/search/scientificName" controller: "search", action: "findByScientificName"
        "/profile/search/taxon/name" controller: "search", action: "findByTaxonNameAndLevel"
        "/profile/search/taxon/level" controller: "search", action: "groupByTaxonLevel"
        "/profile/search/taxon/levels" controller: "search", action: "getTaxonLevels"

        "/opus/$opusId/profile/" controller: "profile", action: [GET: "index", PUT: "createProfile"]

        "/opus/$opusId/profile/$profileId" controller: "profile", action: [GET: "getByUuid", DELETE: "deleteProfile", POST: "updateProfile"]

        "/opus/$opusId/profile/$profileId/toggleDraftMode/" controller: "profile", action: [POST: "toggleDraftMode"]
        "/opus/$opusId/profile/$profileId/discardDraftChanges/" controller: "profile", action: [POST: "discardDraftChanges"]
        "/opus/$opusId/profile/$profileId/rename/" controller: "profile", action: [POST: "renameProfile"]

        "/opus/$opusId/profile/$profileId/attribute/" controller: "attribute", action: [GET: "index", PUT: "create", POST: "create"]
        "/opus/$opusId/profile/$profileId/attribute/$attributeId" controller: "attribute", action: [GET: "show", PUT: "update", DELETE: "delete", POST: "update"]

        "/opus/$opusId/profile/$profileId/links" controller: "profile", action: [POST: "saveLinks"]

        "/opus/$opusId/profile/$profileId/bhl" controller: "profile", action: [POST: "saveBHLLinks"]

        "/opus/$opusId/profile/$profileId/recordStagedImage" controller: "profile", action: [POST: "recordStagedImage"]

        "/opus/$opusId/profile/$profileId/authorship" controller: "profile", action: [POST: "saveAuthorship"]

        "/opus/$opusId/profile/$profileId/bibliography" controller: "profile", action: [POST: "saveBibliography"]

        "/opus/$opusId/profile/$profileId/specimen" controller: "profile", action: [POST: "saveSpecimens"]

        "/opus/$opusId/profile/$profileId/classification" controller: "profile", action: [GET: "classification"]

        "/opus/$opusId/profile/$profileId/publication" controller: "profile", action: [GET: "listPublications", POST: "savePublication"]
        "/opus/$opusId/profile/$profileId/publication/$publicationId/file" controller: "profile", action: [GET: "getPublicationFile"]

        "/opus/$opusId/profile/$profileId/comment" controller: "comment", action: [GET: "getComments", PUT: "addComment"]
        "/opus/$opusId/profile/$profileId/comment/$commentId" controller: "comment", action: [GET: "getComment", POST: "updateComment", DELETE: "deleteComment"]

        "/opus/$opusId/archive/$profileId" controller: "profile", action: [POST: "archiveProfile"]
        "/opus/$opusId/restore/$profileId" controller: "profile", action: [POST: "restoreArchivedProfile"]

        "/checkName" controller: "profile", action: [GET: "checkName"]

        "/report/archivedProfiles" controller: "report", action: [GET: "archivedProfiles"]
        "/report/draftProfiles" controller: "report", action: [GET: "draftProfiles"]
        "/report/mismatchedNames" controller: "report", action: [GET: "mismatchedNames"]
        "/report/recentChanges" controller: "report", action: [GET: "recentChanges"]

        "/statistics/" controller: "statistics", action: [GET: "index"]

        "/status/" controller: "status", action: [GET: "status"]
        "/status/$component" controller: "status", action: [GET: "status"]

        "/publication/$publicationId" controller: "profile", action: [GET: "getPublicationDetails"]

        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/" view: "/index"
        "500" view: '/error'
    }
}
