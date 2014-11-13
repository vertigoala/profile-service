package au.org.ala.profile

class Opus {

    String uuid
    String title
    String dataResourceUid
    List imageSources            // a list of drs that are providing images we can include
    List recordSources         // a list of drs that are providing images we can include

    static constraints = {}

    static mapping = {
        version false
    }
}
