package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import au.org.ala.profile.security.Role
import au.org.ala.profile.util.ShareRequestAction
import au.org.ala.web.AuthService
import grails.converters.JSON

@RequireApiKey
class OpusController extends BaseController {

    OpusService opusService
    AuthService authService

    def index() {
        render Opus.findAll() as JSON
    }

    def show() {
        def result = getOpus()
        if (result) {
            int profiles = Profile.countByOpus(result)
            result.profileCount = profiles

            render result as JSON
        } else {
            notFound()
        }
    }

    def create() {
        Opus opus = opusService.createOpus(request.getJSON())
        render opus as JSON
    }

    def deleteOpus() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                boolean success = opusService.deleteOpus(opus.uuid)

                render([success: success] as JSON)
            }
        }
    }

    def updateOpus() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                def json = request.getJSON()

                boolean updated = opusService.updateOpus(opus.uuid, json)

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    def updateSupportingOpuses() {
        def json = request.getJSON()
        if (!params.opusId || !json) {
            badRequest "opusId and a json body are required"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for id ${params.opusId}"
            } else {
                boolean updated = opusService.updateSupportingOpuses(opus.uuid, json)

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    def respondToSupportingOpusRequest() {
        if (!params.requestingOpusId || !params.opusId || !params.requestAction || !ShareRequestAction.valueOf(params.requestAction.toUpperCase())) {
            badRequest "requestingOpusId, supportingOpusId and accept are required parameters"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for id ${params.opusId}"
            } else {
                boolean updated = opusService.respondToSupportingOpusRequest(opus.uuid, params.requestingOpusId, ShareRequestAction.valueOf(params.requestAction.toUpperCase()))

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    def updateUserAccess() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                def json = request.getJSON()

                boolean updated = opusService.updateUserAccess(opus.uuid, json)

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
            }
        }
    }

    def generateAccessToken() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                String token = opusService.generateAccessToken(opus.uuid)

                render ([token: token] as JSON)
            }
        }
    }

    def revokeAccessToken() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                opusService.revokeAccessToken(opus.uuid)

                render ([revoked: true] as JSON)
            }
        }
    }

    def getGlossary() {
        Glossary glossary = null

        if (!params.opusId && !params.glossaryId) {
            badRequest "You must provide either an opusId or a glossaryId"
        } else if (params.opusId) {
            Opus opus = getOpus()

            if (!opus || !opus.glossary) {
                notFound()
            } else {
                glossary = opus.glossary
            }
        } else {
            glossary = Glossary.findByUuid(params.glossaryId)
        }

        if (!glossary) {
            notFound()
        } else {
            List<GlossaryItem> items = GlossaryItem.findAllByGlossaryAndTermLike(glossary, "${params.prefix ?: ''}%")

            glossary.items = items
        }

        render glossary as JSON
    }

    def saveGlossaryItems() {
        def json = request.getJSON();
        if (!json || (!json.opusId && !json.glossaryId) || !params.opusId) {
            badRequest()
        } else {
            Opus opus = getOpus()
            boolean updated = opusService.saveGlossaryItems(opus.uuid, json)

            if (!updated) {
                saveFailed()
            } else {
                success([updated: updated])
            }
        }
    }

    def deleteGlossaryItem() {
        if (!params.glossaryItemId) {
            badRequest()
        } else {
            boolean deleted = opusService.deleteGlossaryItem(params.glossaryItemId)

            if (!deleted) {
                saveFailed()
            } else {
                success([deleted: deleted])
            }
        }
    }

    def updateGlossaryItem() {
        def json = request.getJSON();

        if (!params.glossaryItemId || !json) {
            badRequest()
        } else {
            boolean updated = opusService.updateGlossaryItem(params.glossaryItemId, json)

            if (!updated) {
                saveFailed()
            } else {
                success([updated: updated])
            }
        }
    }

    def createGlossaryItem() {
        def json = request.getJSON();

        if (!params.opusId || !json) {
            badRequest()
        } else {
            Opus opus = getOpus()
            if (!opus) {
                notFound()
            } else {
                GlossaryItem item = opusService.createGlossaryItem(opus.uuid, json)

                if (!item) {
                    saveFailed()
                } else {
                    render item as JSON
                }
            }
        }
    }

    def about() {
        if (!params.opusId) {
            badRequest()
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                render ([opus: [title: opus.title,
                                opusId: opus.uuid,
                                aboutHtml: opus.aboutHtml,
                                citationHtml: opus.citationHtml,
                                copyright: opus.copyrightText,
                                administrators: opus.authorities.collect {
                                    if (it.role == Role.ROLE_PROFILE_ADMIN) {
                                        [email: authService.getUserForUserId(it.user.userId, false)?.userName,
                                         name: it.user.name]
                                    }
                                }]] as JSON)
            }
        }
    }

    def updateAbout() {
        def json = request.getJSON()
        if (!params.opusId || !json || !json.containsKey("aboutHtml") || !json.containsKey("citationHtml")) {
            badRequest()
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                opus = opusService.updateAbout(opus.uuid, json)

                render ([opus: [title: opus.title, opusId: opus.uuid, aboutHtml: opus.aboutHtml]] as JSON)
            }
        }
    }
}
