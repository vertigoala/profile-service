package au.org.ala.profile

class DoiService {

    String mintDOI(Publication publication) {
        return "tempDOI_${System.currentTimeMillis()}"
    }
}
