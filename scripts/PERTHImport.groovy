@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
@Grab('org.apache.commons:commons-lang3:3.3.2')

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.apache.commons.lang3.StringUtils
import org.apache.http.impl.client.LaxRedirectStrategy

class PERTHImport {

	static void main(args) {
		def cli = new CliBuilder(usage: "groovy PERTHImport -o <opusId> -f <perthServiceBaseUrl> -u <floraBaseUser> -p <floraBasePass> -s <profileServiceBaseUrl> [-r <reportFile>]")
		cli.o(longOpt: "opusId",
				"UUID of the PERTH Flora Opus",
				required: true, args: 1)
		cli.f(longOpt: "perthServiceBaseUrl",
				"Base URL of the PERTH web service (in FloraBase)",
				required: true, args: 1)
		cli.u(longOpt: "floraBaseUser",
				"Username of the FloraBase account to use to access web services",
				required: true, args: 1)
		cli.p(longOpt: "floraBasePass",
				"Password of the FloraBase account to use to access web services",
				required: true, args: 1)
		cli.s(longOpt: "profileServiceBaseUrl",
				"Base URL of the profile service",
				required: true, args: 1)
		cli.r(longOpt: "reportFile",
				"File to write the results of the import to",
				required: false, args: 1)

		OptionAccessor opt = cli.parse(args)
		if (!opt) {
			return
		}

		String OPUS_ID = opt.o
		String FLORABASE_USER = opt.u
		String FLORABASE_PASS = opt.p
		String PERTH_BASE_URL = StringUtils.removeEnd(opt.f, "/");
		String PROFILE_SERVICE_IMPORT_URL = StringUtils.removeEnd(opt.s, "/") + "/import/profile";
		String REPORT_FILE = opt.r ?: "report.txt"
		String charset = "UTF-8"

		Object data = requestData(PERTH_BASE_URL, FLORABASE_USER, FLORABASE_PASS)
		if (data) {
			Map opus = [
			        opusId: OPUS_ID,
					profiles: data
			]

			println(opus)

			postOpus(opus, PROFILE_SERVICE_IMPORT_URL)
		}
	}

	static Object requestData(String url, String username, String password) {
		println(url)
		String PERTH_PROFILE_SERVICE = "/ws/profile/list"
		String PERTH_LOGIN_SERVICE = "${url}/login"

		RESTClient service = new RESTClient(url)
		service.client.setRedirectStrategy(new LaxRedirectStrategy())
		def response = service.post(
				requestContentType: ContentType.JSON,
				path: PERTH_LOGIN_SERVICE,
				query: [
						username: username,
						password: password,
						p: PERTH_PROFILE_SERVICE,
						process: 1
				])

		if (response.status == 200) {
			return response.data.profiles
		}
		else {
			println "Import failed with HTTP ${response.status}"
			return null
		}
	}

	static void postOpus(Map data, String url) {
		println(url)
		RESTClient service = new RESTClient(url)
		def response = service.post(body: data, requestContentType: ContentType.JSON)

		if (response.status == 200) {
			println("Data loaded")
		}
		else if (response.status == 400) {
			println response.data
		}
		else if (response.status == 404) {
			println response.data
		}
		else {
			println "Import failed with HTTP ${response.status}"
		}
	}
}
