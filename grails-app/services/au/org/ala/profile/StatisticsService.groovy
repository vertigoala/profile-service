package au.org.ala.profile

/**
 * StatisticsService
 *
 * Bundles up the queries needed to generate per-collection (opus) statistics.
 *
 * @author Ben Richardson
 * @version $Revision$
 */
class StatisticsService {

	Profile lastEditedProfile(Opus opus) {
		Profile profile = Profile.withCriteria {
			eq("opus", opus)
			maxResults(1)
			order("lastUpdated", "desc")
		}.first()

		return profile
	}
}
