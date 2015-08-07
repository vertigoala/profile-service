package au.org.ala.profile
import au.org.ala.profile.security.Role
import grails.converters.JSON

class StatisticsController extends BaseController {

	StatisticsService statisticsService
	ReportService reportService

	def index() {
		if (!params.opusId) {
			badRequest "opusId is a required parameter"
		} else {
			Opus opus = getOpus()

			if (!opus) {
				notFound "No opus found for ${params.opusId}"
			} else {
				def statistics = []

				int profileCount = Profile.createCriteria().count {
					eq("opus", opus)
				}
				statistics.add([
						id: 'profileCount',
				        name: 'Profiles',
						value: profileCount
				])

				int editorCount = opus.authorities.count {
					eq("role", Role.ROLE_PROFILE_EDITOR)
				}
				statistics.add([
						id: 'editorCount',
				        name: 'Editors',
						value: editorCount
				])

				Date from = new Date().minus(30)
				Date to = new Date()
				Map updatesThisMonth = reportService.recentUpdates(opus.uuid, from, to, 0, 1, true)
				statistics.add([
						id: 'updatesThisMonth',
						name: 'Updates This Month',
						value: updatesThisMonth.recordCount
				])

				Profile profile = statisticsService.lastEditedProfile(opus)
				statistics.add([
						id: 'lastEditedProfile',
				        name: 'Last Edited',
						value: "${profile.scientificName} by ${profile.lastUpdatedBy} on ${profile.lastUpdated.format("dd MMM yyyy")}"
				])

				int namesNotInNSL = statisticsService.profilesWithNameNotInNSL(opus)
				statistics.add([
						id: 'namesNotInNSL',
				        name: 'Names Not In NSL',
						value: namesNotInNSL
				])

				float namesNotInNSLAsPercent = statisticsService.profilesWithNameNotInNSLAsPercent(opus)
				statistics.add([
						id: 'namesNotInNSLPercent',
				        name: 'Names Not In NSL Percent',
						value: namesNotInNSLAsPercent,
				])

				render statistics as JSON
			}
		}
	}
}
