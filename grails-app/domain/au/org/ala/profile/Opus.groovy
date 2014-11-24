package au.org.ala.profile

class Opus {

    String uuid
    String title
    String dataResourceUid
    List<String> imageSources            // a list of drs that are providing images we can include
    List<String>  recordSources         // a list of drs that are providing images we can include
    String logoUrl
    String bannerUrl
    String attributeVocabUuid

    static constraints = {}

    static mapping = {
        version false
        logoUrl nullable: true
        bannerUrl nullable: true
        attributeVocabUuid nullable: true
    }
}
