package au.org.ala.profile

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(UserSettings)
class UserSettingsSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test enableFlorulaList"() {
        UserSettings userSettings = new UserSettings(id: 'a')
        String opusId = 'opus'
        String listId = 'list'
        userSettings.enableFlorulaList(opusId, listId)
        userSettings.allFlorulaSettings[opusId].drUid == listId
    }
}
