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
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        if (!json) {
            badRequest()
        } else {
            List<String> linkIds = profileService.saveBHLLinks(json.profileId, json)

            render linkIds as JSON
        }
    }

    def saveLinks() {
        def jsonSlurper = new JsonSlurper()
        def json = jsonSlurper.parse(request.getReader())

        if (!json) {
            badRequest()
        } else {
            List<String> linkIds = profileService.saveLinks(json.profileId, json)

            render linkIds as JSON
        }
    }

    def saveAuthorship() {
        def json = request.getJSON()

        if (!params.profileId || !json) {
            badRequest()
        } else {
            boolean saved = profileService.saveAuthorship(params.profileId, json)

            if (saved) {
                Profile profile = Profile.findByUuid(params.profileId)
                render profile.authorship as JSON
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
            Publication pub = profileService.savePublication(params.profileId, publication, file)

            render pub as JSON
        }
    }

    def getPublicationFile() {
        if (!params.publicationId) {
            badRequest "publicationId is a required parameter"
        } else {
            GridFSDBFile file = profileService.getPublicationFile(params.publicationId)

            response.setContentType("application/pdf")
            response.setHeader("Content-disposition", "attachment;filename=publication.pdf")
            file.writeTo(response.outputStream)
        }
    }

    def deletePublication() {
        if (!params.publicationId) {
            badRequest "publicationId is a required parameter"
        } else {
            boolean success = profileService.deletePublication(params.publicationId)

            respond success, [formats: ["json", "xml"]]
        }
    }

    def listPublications() {
        if (!params.profileId) {
            badRequest()
        } else {
            Set<Publication> publications = profileService.listPublications(params.profileId as String)

            render publications as JSON
        }
    }

    def index() {
        log.debug("ProfileController.index")
        def results = Profile.findAll([max: 100], {})
        render(contentType: "application/json") {
            if (results) {
                array {
                    results.each { tp ->
                        taxon(
                                "profileId": "${tp.uuid}",
                                "guid": "${tp.guid}",
                                "scientificName": "${tp.scientificName}"
                        )
                    }
                }
            } else {
                []
            }
        }
    }

    def classification() {
        if (!params.guid || !params.opusId) {
            badRequest "GUID and OpusId are required parameters"
        } else {
            log.debug("Retrieving classification for ${params.guid} in opus ${params.opusId}")
            JsonSlurper js = new JsonSlurper()
            def classification = bieService.getClassification(params.guid)

            Opus opus = Opus.findByUuid(params.opusId)

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
            Opus opus = Opus.findByUuid(json.opusId)
            if (!opus) {
                notFound "No matching opus can be found"
            } else {
                Profile profile = Profile.findByScientificNameAndOpus(json.scientificName, opus)

                if (profile) {
                    sendError HttpStatus.SC_NOT_ACCEPTABLE, "A profile already exists for ${json.scientificName}"
                } else {
                    profile = profileService.createProfile(json.opusId, json);
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
            Profile profile = Profile.findByUuid(params.profileId)

            if (!profile) {
                notFound()
            } else {
                profile = profileService.updateProfile(params.profileId, json)

                render profile as JSON
            }
        }
    }

    def getByUuid() {
        // TODO do this better
        log.debug("Fetching profile by profileId ${params.profileId}")
        Profile tp = Profile.findByUuidOrGuidOrScientificName(params.profileId, params.profileId, params.profileId)

        if (tp) {
            respond tp, [formats: ['json', 'xml']]
        } else {
            notFound()
        }
    }

    def deleteProfile() {
        if (!params.profileId) {
            badRequest "profileId is a required parameter"
        } else {
            boolean success = profileService.deleteProfile(params.profileId)

            respond success, [formats: ["json", "xml"]]
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
