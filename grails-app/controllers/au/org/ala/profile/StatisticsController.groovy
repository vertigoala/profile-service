package au.org.ala.profile

import au.org.ala.profile.security.Role
import grails.converters.JSON

class StatisticsController extends BaseController {

	StatisticsService statisticsService

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
				        name: 'Profiles',
						value: profileCount
				])

				int editorCount = opus.authorities.count {
					eq("role", Role.ROLE_PROFILE_EDITOR)
				}
				statistics.add([
				        name: 'Editors',
						value: editorCount
				])

				Profile profile = statisticsService.lastEditedProfile(opus)
				statistics.add([
				        name: 'Last Edited',
						value: "${profile.scientificName} by ${profile.lastUpdatedBy} on ${profile.lastUpdated.format("dd MMM yyyy")}"
				])

				int namesNotInNSL = statisticsService.profilesWithNameNotInNSL(opus)
				statistics.add([
				        name: 'Names Not In NSL',
						value: namesNotInNSL
				])

				float namesNotInNSLAsPercent = statisticsService.profilesWithNameNotInNSLAsPercent(opus)
				statistics.add([
				        name: 'Names Not In NSL Percent',
						value: namesNotInNSLAsPercent,
				])

				render statistics as JSON
			}
		}
	}
}
