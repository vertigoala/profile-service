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
    Boolean enablePhyloUpload = true
    Boolean enableOccurrenceUpload = true
    Boolean enableTaxaUpload = true
    Boolean enableKeyUpload = true

    static constraints = {
        logoUrl nullable: true
        bannerUrl nullable: true
        attributeVocabUuid nullable: true
        enablePhyloUpload nullable: true
        enableOccurrenceUpload nullable: true
        enableTaxaUpload nullable: true
        enableKeyUpload nullable: true
    }

    static mapping = {
        version false
    }
}
