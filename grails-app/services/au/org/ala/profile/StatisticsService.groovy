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
		List<Profile> profiles = Profile.withCriteria {
			eq("opus", opus)
			maxResults(1)
			order("lastUpdated", "desc")
		}

        if (profiles) {
            profiles.first()
        } else {
            null
        }
	}
}
