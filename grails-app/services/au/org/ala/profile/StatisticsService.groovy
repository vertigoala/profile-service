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

	float profilesWithNameNotInNSLAsPercent(Opus opus) {
		int profileCount = Profile.countByOpus(opus)
		int profilesWithoutNSLName = profilesWithNameNotInNSL(opus)

		return profilesWithoutNSLName / profileCount * 100.0;
	}

	int profilesWithNameNotInNSL(Opus opus) {
		return Profile.createCriteria().count {
			eq("opus", opus)
			isNull("nslIdentifier")
		}
	}
}
