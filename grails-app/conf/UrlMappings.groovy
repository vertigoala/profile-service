class UrlMappings {

	static mappings = {

        "/opus/$uuid"(controller: "opus", action: "show")

        "/importFOA"(controller: "profile", action: "importFOA")

        "/profile/search"(controller: "profile", action: "search")

        "/profile/$uuid"(controller: "profile", action: "getByUuid")

        "/profile/"(controller: "profile", action: "index")

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
	}
}
