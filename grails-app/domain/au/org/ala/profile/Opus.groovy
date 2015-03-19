package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Opus {

    String uuid
    String title
    String dataResourceUid
    List<String> imageSources            // a list of drs that are providing images we can include
    List<String> recordSources         // a list of drs that are providing images we can include
    String logoUrl
    String bannerUrl
    String mapAttribution // e.g. AVH (CHAH)
    String mapPointColour = "FF9900"
    Float mapDefaultLatitude = -23.6
    Float mapDefaultLongitude = 133.6
    Integer mapZoom = 3
    String mapBaseLayer = "https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png"
    String biocacheUrl    // e.g.  http://avh.ala.org.au/
    String biocacheName    ///e.g. Australian Virtual Herbarium
    String attributeVocabUuid
    Boolean enablePhyloUpload = false
    Boolean enableOccurrenceUpload = false
    Boolean enableTaxaUpload = false
    Boolean enableKeyUpload = false
    Boolean showLinkedOpusAttributes = false
    Boolean allowCopyFromLinkedOpus = false

    static hasMany = [additionalOccurrenceResources: OccurrenceResource, admins: Contributor, editors: Contributor, supportingOpuses: Opus]

    static constraints = {
        logoUrl nullable: true
        bannerUrl nullable: true
        attributeVocabUuid nullable: true
        enablePhyloUpload nullable: true
        enableOccurrenceUpload nullable: true
        enableTaxaUpload nullable: true
        enableKeyUpload nullable: true
        mapAttribution nullable: true
        biocacheUrl nullable: true
        biocacheName nullable: true
    }

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static mapping = {
        version false
    }
}
