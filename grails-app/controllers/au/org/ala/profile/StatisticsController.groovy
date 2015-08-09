package au.org.ala.profile
import au.org.ala.profile.security.Role
import grails.converters.JSON

import java.text.SimpleDateFormat

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
						value: profileCount,
                        tooltip: "Total number of profiles in this collection"
				])

                Map mismatchedNames = reportService.mismatchedNames(opus.uuid, 0, 1, true)
                statistics.add([
                        id: 'mismatchedNames',
                        name: 'Mismatched Names',
                        value: mismatchedNames.recordCount,
                        tooltip: "Number of profiles where the name does not exist in the National Species List (NSL)"
                ])

                float mismatchedNamesAsPercent = mismatchedNames.recordCount / profileCount * 100.0;
                statistics.add([
                        id: 'mismatchedNamesPercent',
                        name: 'Mismatched Names %',
                        value: "${mismatchedNamesAsPercent.round(2)}%",
                        tooltip: "Percentage of profiles where the name does not exist in the National Species List (NSL)"
                ])

				int editorCount = opus.authorities.count {
					it.role == Role.ROLE_PROFILE_EDITOR
				}
				statistics.add([
						id: 'editorCount',
				        name: 'Editors',
						value: editorCount,
                        tooltip: "Number of people who can edit profiles in this collection"
                ])

				Calendar from = Calendar.getInstance()
                from.set(Calendar.DAY_OF_MONTH, 1)
                from.set(Calendar.HOUR_OF_DAY, 0)
                from.set(Calendar.MINUTE, 0)
                from.set(Calendar.SECOND, 0)
                from.set(Calendar.MILLISECOND, 0)

				Date to = new Date()
				Map updatesThisMonth = reportService.recentUpdates(opus.uuid, from.getTime(), to, 0, 1, true)
				statistics.add([
						id: 'updatesThisMonth',
						name: 'Updates This Month',
						value: updatesThisMonth.recordCount,
                        tooltip: "Number of profiles which have been updated since ${from.getTime().format("dd/MM/yyyy")}"
                ])

				Profile profile = statisticsService.lastEditedProfile(opus)
				statistics.add([
						id: 'lastUpdatedProfile',
				        name: 'Last Updated',
						value: "${profile.scientificName}",
                        tooltip: "Updated by ${profile.lastUpdatedBy} on ${profile.lastUpdated.format('dd/MM/yyyy')}"
				])

				render statistics as JSON
			}
		}
	}
}
