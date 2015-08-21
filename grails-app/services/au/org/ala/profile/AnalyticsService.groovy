package au.org.ala.profile
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.analytics.Analytics
import com.google.api.services.analytics.AnalyticsScopes
import com.google.api.services.analytics.model.GaData

class AnalyticsService {

	private static final JSON_FACTORY = JacksonFactory.getDefaultInstance()
	private Analytics analytics
	def grailsApplication
	private String viewIds

	def AnalyticsService() {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport()
		GoogleCredential credential = new GoogleCredential.Builder()
			.setTransport(httpTransport)
			.setJsonFactory(JSON_FACTORY)
			.setServiceAccountId("${grailsApplication.config.analytics.serviceAccountEmail}")
			.setServiceAccountPrivateKeyFromP12File(new File("${grailsApplication.config.analytics.p12.file}"))
			.setServiceAccountScopes(Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
			.build()

		analytics = new Analytics.Builder(httpTransport, JSON_FACTORY, credential)
			.setApplicationName("${grailsApplication.config.application.name}")
			.build()

		viewIds = "ga:${grailsApplication.config.analytics.viewId}"
	}

	/**
	 * Query Google Analytics for the most interesting statistics on profiles.
	 * <p>Pageviews for this profile by key are:
	 * <ul>
	 *     <li>{@code allTime}, representing the pageview count for this profile since
	 *     counting began,</li>
	 *     <li>{@code 30Days}, representing the pageview count for the last 30 days,
	 *     and</li>
	 *     <li>{@code 7Days}, representing the pageview count for the last 7 days</li>
	 * </ul>
	 * <p>A further summary statistic included is {@code mostViewedProfile}, representing
	 * a count of the number of pageviews and the name of the lifeform associated with
	 * the most viewed profile in the opus.</p>
	 *
	 * @param opus the opus of interest (only for mostViewedProfile at this stage)
	 * @param profile the profile of interest
	 */
	Map analyticsByProfile(Opus opus, Profile profile) {
		Map ret = [:]

		//def profileUri = "/opus/${profile.opus.uuid}/profile/${profile.uuid}"
		def profileUri = "/browse/profile/"

		// Query Google Analytics for ga:sessions and ga:pageviews...

		// All time
		ret.allTime = queryForViews(profileUri, '2005-01-01')

		// Last 30 days
		ret.last30Days = queryForViews(profileUri, "30daysAgo")

		// Last 7 days
		ret.last7Days = queryForViews(profileUri, "7daysAgo")

		// Most viewed profile
		ret.mostViewedProfile = queryMostViewedProfile()

		return ret
	}

	/**
	 * Perform a Google Analytics query for the sessions and pageviews received by a
	 * eFlora profile.
	 * @param profileUri the uri (rooted at the context) of the profile to query
	 * @param startDate the date on which to start the query (endDate is always "today")
	 * @return
	 */
	private Map queryForViews(String profileUri, String startDate) {
		def metrics = "ga:sessions,ga:pageviews"
		def filters = "ga:pagePath==${profileUri}"
		GaData result = analytics.data().ga().get(viewIds, startDate, "today", metrics)
				.setFilters(filters)
				.execute()

		return [
		        pageviews: result.get('ga:pageviews'),
				sessions: result.get('ga:sessions')
		]
	}

	/**
	 * Perform a Google Analytics query for the most viewed profile (of all time).
	 * @return the data, containing ga:pageviews count for each profile in descending
	 * order
	 */
	private Map queryMostViewedProfile() {
		def metrics = "ga:pageviews"
		def dimensions = "ga:pagePath"
		def sort = "-ga:pageviews"
		def filters = "ga:pagePath=~^/opus/.*/profile/.*"
		GaData result = analytics.data().ga().get(viewIds, "2005-01-01", "today", metrics)
			.setDimensions(dimensions)
			.setSort(sort)
			.setFilters(filters)
			.execute()

		return [
				pageviews: result.get('ga:pageviews'),
				sessions: result.get('ga:sessions')
		]
	}
}
