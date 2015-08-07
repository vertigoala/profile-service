import grails.test.AbstractCliTestCase

class PERTHImportTests extends AbstractCliTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testPERTHImport() {

        execute(["perthi-mport"])

        assertEquals 0, waitForProcess()
        verifyHeader()
    }
}
