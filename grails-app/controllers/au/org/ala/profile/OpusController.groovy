package au.org.ala.profile

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON

@RequireApiKey
class OpusController extends BaseController {

    OpusService opusService

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

    def updateUsers() {
        if (!params.opusId) {
            badRequest "You must provide an opusId"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                def json = request.getJSON()

                boolean updated = opusService.updateUsers(opus.uuid, json)

                if (!updated) {
                    saveFailed()
                } else {
                    success([updated: true])
                }
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
                render ([opus: [title: opus.title, opusId: opus.uuid, aboutHtml: opus.aboutHtml]] as JSON)
            }
        }
    }

    def updateAboutHtml() {
        def json = request.getJSON()
        if (!params.opusId || !json || !json.containsKey("aboutHtml")) {
            badRequest()
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound()
            } else {
                opus = opusService.updateAboutHtml(opus.uuid, json.aboutHtml)

                render ([opus: [title: opus.title, opusId: opus.uuid, aboutHtml: opus.aboutHtml]] as JSON)
            }
        }
    }
}
