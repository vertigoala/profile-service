package au.org.ala.profile

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Created by shi131 on 1/03/2016.
 */
@TestFor(ProfileService)
class ProfileServiceUnitSpec extends Specification {

    def "Filename will have correct extension"() {

        given: "an absolute file name with extension"
        String originalName = '/data/snapshot/445.pdf'
        String expectedExtension = 'zip'
        when: "we check the extension"
        String finalName = service.makeSureFileExtensionIsCorrect(expectedExtension, originalName)
        then: "the final extension will be what we want"
        finalName.substring(finalName.indexOf('.') + 1) == expectedExtension
    }

    def "Filename will include absolute path"() {
        given: "an absolute file name"
        String originalName = '/data/snapshot/445.pdf'
        String expectedExtension = 'zip'
        when: "we check the extension"
        String finalName = service.makeSureFileExtensionIsCorrect(expectedExtension, originalName)
        then: "we retain the absolute path"
        finalName.contains('/data/snapshot')
        finalName.equalsIgnoreCase('/data/snapshot/445.zip')
    }

}
