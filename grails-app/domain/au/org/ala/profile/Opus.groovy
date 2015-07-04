package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Opus {

    String uuid
    String shortName
    String title
    String description
    String dataResourceUid
    List<String> imageSources            // a list of drs that are providing images we can include
    List<String> recordSources         // a list of drs that are providing images we can include
    List<String> approvedLists
    String logoUrl
    String bannerUrl
    String thumbnailUrl
    String mapAttribution // e.g. AVH (CHAH)
    String mapPointColour = "FF9900"
    Float mapDefaultLatitude = -23.6
    Float mapDefaultLongitude = 133.6
    Integer mapZoom = 3
    String mapBaseLayer = "https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png"
    String biocacheUrl    // e.g.  http://avh.ala.org.au/
    String biocacheName    ///e.g. Australian Virtual Herbarium
    String attributeVocabUuid
    String authorshipVocabUuid
    Boolean enablePhyloUpload = false
    Boolean enableOccurrenceUpload = false
    Boolean enableTaxaUpload = false
    Boolean enableKeyUpload = false
    Boolean showLinkedOpusAttributes = false
    Boolean allowCopyFromLinkedOpus = false
    Boolean allowFineGrainedAttribution = true
    Glossary glossary
    String keybaseProjectId
    String keybaseKeyId
    String aboutHtml
    String copyrightText
    String footerText
    String email
    String facebook
    String twitter
    List<String> excludeRanksFromMap

    static hasMany = [additionalOccurrenceResources: OccurrenceResource, authorities: Authority, supportingOpuses: Opus]

    static constraints = {
        shortName nullable: true
        description nullable: true
        logoUrl nullable: true
        bannerUrl nullable: true
        thumbnailUrl nullable: true
        attributeVocabUuid nullable: true
        authorshipVocabUuid nullable: true
        enablePhyloUpload nullable: true
        enableOccurrenceUpload nullable: true
        enableTaxaUpload nullable: true
        enableKeyUpload nullable: true
        mapAttribution nullable: true
        biocacheUrl nullable: true
        biocacheName nullable: true
        keybaseProjectId nullable: true
        keybaseKeyId nullable: true
        aboutHtml nullable: true
        copyrightText nullable: true
        footerText nullable: true
        email nullable: true
        facebook nullable: true
        twitter nullable: true
    }

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static mapping = {
        version false
        glossary cascade: "all-delete-orphan"
        authorities cascade: "all-delete-orphan"
    }
}
