package au.org.ala
/**
 *
 */
class ProfileIntegrationSpec extends GroovyTestCase {

    def profileService

    def setup() {}

    def cleanup() {}

    void testCreateProfile() {
        log.info("Importing FOA")
        profileService.importFOA()
        log.info("Importing FOA - done")
    }
}
