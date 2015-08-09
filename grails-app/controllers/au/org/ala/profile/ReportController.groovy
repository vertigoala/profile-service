package au.org.ala.profile
import grails.converters.JSON

class ReportController extends BaseController {
    def reportService

    def draftProfiles() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for ${params.opusId}"
            } else {
                Map report = reportService.draftProfiles(opus.uuid)

                render report as JSON
            }
        }
    }

    def archivedProfiles() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for ${params.opusId}"
            } else {
                Map report = reportService.archivedProfiles(opus.uuid)

                render report as JSON
            }
        }
    }

    def mismatchedNames() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for ${params.opusId}"
            } else {
                int max = params.max && params.max != "null" ? params.max as int : -1
                int startFrom = params.offset ? params.offset as int : 0

                Map report = reportService.mismatchedNames(opus.uuid, max, startFrom, false)

                render report as JSON
            }
        }
    }

    def recentChanges() {
        if (!params.opusId || !params.from || !params.to) {
            badRequest "opusId, from and to date are required parameters"
        } else {
            Opus opus = getOpus()

            int max = params.max && params.max != "null" ? params.max as int : -1
            int startFrom = params.offset ? params.offset as int : 0
            boolean countOnly = params.countOnly?.toBoolean()

            try {
                Date from = new Date(params.from);
                Date to = new Date(params.to);
                if (!opus) {
                    notFound "No opus found for ${params.opusId}"
                } else {
                    Map report = reportService.recentUpdates(
                            opus.uuid, from, to, max, startFrom, countOnly);
                    render report as JSON
                }
            } catch (Exception e) {
                badRequest "Provided date is not in the correct format"
            }
        }
    }
}
