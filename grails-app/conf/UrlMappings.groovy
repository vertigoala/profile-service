class UrlMappings {

	static mappings = {


        "/classification"(controller: "profile", action: [GET:"classification"])

        "/vocab/"(controller: "vocab", action: "index")

        "/vocab/$uuid"(controller: "vocab", action: "show")

        "/attribute/"(controller: "attribute", action: [GET:"index", PUT:"create",  POST:"create"])

        "/attribute/$uuid"(controller: "attribute", action: [GET:"show", PUT:"update", DELETE:"delete", POST:"update"])

        "/opus/"(controller: "opus", action: "index")

        "/opus/$uuid"(controller: "opus", action: "show")

        "/opus/taxaUpload"(controller: "opus", action: "taxaUpload")

        "/importFOA"(controller: "profile", action: "importFOA")

        "/importSponges"(controller: "profile", action: "importSponges")

        "/profile/search"(controller: "profile", action: "search")

        "/profile/$uuid"(controller: "profile", action: "getByUuid")

        "/profile/"(controller: "profile", action: "index")

        "/profile/links/$uuid"(controller: "profile", action: [POST:"saveLinks"])

        "/profile/bhl/$uuid"(controller: "profile", action: [POST:"saveBHLLinks"])

        "/createTestOccurrenceSource"(controller: 'profile', action: 'createTestOccurrenceSource')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
	}
}
