package au.org.ala.profile

import com.mongodb.gridfs.GridFSDBFile
import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

class ProfileController extends BaseController {

    ProfileService profileService
    ImportService importService
    BieService bieService

    def saveBHLLinks() {
        def json = request.getJSON()

        if (!json) {
            badRequest()
        } else {
            boolean success = profileService.saveBHLLinks(json.profileId, json)

            render ([success: success] as JSON)
        }
    }

    def saveLinks() {
        def json = request.getJSON()

        if (!json) {
            badRequest()
        } else {
            boolean success = profileService.saveLinks(json.profileId, json)

            render ([success: success] as JSON)
        }
    }

    def saveAuthorship() {
        def json = request.getJSON()

        if (!params.profileId || !json) {
            badRequest()
        } else {
            boolean saved = profileService.saveAuthorship(params.profileId, json)

            if (saved) {
                render ([success: saved] as JSON)
            } else {
                saveFailed()
            }
        }
    }

    def savePublication() {
        def publication
        MultipartFile file
        if (request instanceof MultipartHttpServletRequest) {
            publication = new JsonSlurper().parseText(request.getParameter("data"))
            file = request.getFile("file0")
        }

        // the publicationId may be blank (e.g. when creating a new publication), but the request should still have it
        if (!file || !publication || !params.profileId) {
            badRequest()
        } else {
            Profile profile = getProfile()
            if (!profile) {
                notFound "Profile ${params.profileId} not found"
            } else {
                Publication pub = profileService.savePublication(profile.uuid, publication, file)

                render pub as JSON
            }
        }
    }

    def getPublicationFile() {
        if (!params.publicationId) {
            badRequest "publicationId is a required parameter"
        } else {
            GridFSDBFile file = profileService.getPublicationFile(params.publicationId)

            if (!file) {
                notFound "The requested file could not be found"
            } else {
                response.setContentType("application/pdf")
                response.setHeader("Content-disposition", "attachment;filename=publication.pdf")
                file.writeTo(response.outputStream)
            }
        }
    }

    def deletePublication() {
        if (!params.profileId || !params.publicationId) {
            badRequest "profileId and publicationId are required parameters"
        } else {
            Profile profile = getProfile()
            if (!profile) {
                notFound()
            } else {
                boolean success = profileService.deletePublication(profile.uuid, params.publicationId)

                render ([success: success] as JSON)
            }
        }
    }

    def listPublications() {
        if (!params.profileId) {
            badRequest()
        } else {
            Profile profile = getProfile()
            if (!profile) {
                notFound "Profile ${params.profileId} not found"
            } else {
                Set<Publication> publications = profileService.listPublications(profile.uuid)

                render publications as JSON
            }
        }
    }

    def classification() {
        if (!params.guid || !params.opusId) {
            badRequest "GUID and OpusId are required parameters"
        } else {
            log.debug("Retrieving classification for ${params.guid} in opus ${params.opusId}")

            def classification = bieService.getClassification(params.guid)

            Opus opus = getOpus()

            if (!opus) {
                notFound "No matching Opus was found"
            } else {
                classification.each {
                    def profile = Profile.findByGuidAndOpus(it.guid, opus)
                    it.profileUuid = profile?.uuid ?: ''
                }
            }

            response.setContentType("application/json")
            render classification as JSON
        }
    }

    def createProfile() {
        def json = request.getJSON()

        if (!json || !json.scientificName || !json.opusId) {
            badRequest "A json body with at least the scientificName and an opus id is required"
        } else {
            Opus opus = getOpus()
            if (!opus) {
                notFound "No matching opus can be found"
            } else {
                Profile profile = Profile.findByScientificNameAndOpus(json.scientificName, opus)

                if (profile) {
                    sendError HttpStatus.SC_NOT_ACCEPTABLE, "A profile already exists for ${json.scientificName}"
                } else {
                    profile = profileService.createProfile(opus.uuid, json);
                    render profile as JSON
                }
            }
        }
    }

    def updateProfile() {
        def json = request.getJSON()

        if (!json || !params.profileId) {
            badRequest()
        } else {
            Profile profile = getProfile()

            if (!profile) {
                notFound()
            } else {
                profileService.updateProfile(profile.uuid, json)

                profile = getProfile()

                if (profile && profile.draft && params.latest == "true") {
                    Opus opus = profile.opus
                    profile = new Profile(profile.draft.properties)
                    profile.attributes?.each { it.profile = profile }
                    profile.opus = opus
                    profile.privateMode = true
                }

                render profile as JSON
            }
        }
    }

    def toggleDraftMode() {
        if (!params.profileId) {
            badRequest()
        } else {
            Profile profile = getProfile()

            if (!profile) {
                notFound()
            } else {
                profileService.toggleDraftMode(profile.uuid)

                render ([success: true] as JSON)
            }
        }
    }

    def discardDraftChanges() {
        if (!params.profileId) {
            badRequest()
        } else {
            Profile profile = getProfile()

            if (!profile) {
                notFound()
            } else {
                profileService.discardDraftChanges(profile.uuid)

                render ([success: true] as JSON)
            }
        }
    }

    def getByUuid() {
        log.debug("Fetching profile by profileId ${params.profileId}")
        def profile = getProfile()

        if (profile) {
            if (profile && profile.draft && params.latest == "true") {
                Opus opus = profile.opus
                profile = new Profile(profile.draft.properties)
                profile.attributes?.each { it.profile = profile }
                profile.opus = opus
                profile.privateMode = true
            }

            render profile as JSON
        } else {
            notFound()
        }
    }

    def deleteProfile() {
        if (!params.profileId) {
            badRequest "profileId is a required parameter"
        } else {
            Profile profile = getProfile()
            if (!profile) {
                notFound "Profile ${params.profileId} not found"
            } else {
                boolean success = profileService.deleteProfile(profile.uuid)

                render ([success: success] as JSON)
            }
        }
    }

    def importFOA() {
        importService.importFOA()
        render "done"
    }

    def createTestOccurrenceSource() {
        def opus = Opus.findByUuid(profileService.spongesOpusId)

        def testResources = [
                new OccurrenceResource(
                        name: "Test resource 1",
                        webserviceUrl: "http://sandbox.ala.org.au/biocache-service",
                        uiUrl: "http://sandbox.ala.org.au/ala-hub",
                        dataResourceUid: "drt123",
                        pointColour: "CCFF00"
                ),
                new OccurrenceResource(
                        name: "Test resource 2",
                        webserviceUrl: "http://sandbox.ala.org.au/biocache-service",
                        uiUrl: "http://sandbox.ala.org.au/ala-hub",
                        dataResourceUid: "drt125",
                        pointColour: "FFCC00"
                )
        ]

        opus.additionalOccurrenceResources = testResources
        opus.save(flush: true)
    }

    def importFloraBase() {}

    def importSponges() {
        importService.importSponges()
        render "done"
    }
}
