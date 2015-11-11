package au.org.ala.profile

import au.org.ala.profile.util.Utils
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Opus {

    static searchable = {
        root = false
        only = ["title", "uuid", "shortName", "dataResourceUid"]
        title index: "not_analyzed"
        uuid index: "not_analyzed"
        shortName index: "not_analyzed"
        dataResourceUid index: "not_analyzed"
    }

    String uuid
    String shortName
    String title
    String description
    String dataResourceUid
    List<String> imageSources            // a list of drs that are providing images we can include
    List<String> recordSources         // a list of drs that are providing images we can include
    List<String> approvedLists
    List<String> bioStatusLists
    String logoUrl
    String bannerUrl
    String thumbnailUrl
    String mapAttribution // e.g. AVH (CHAH)
    String mapPointColour = "FF9900"
    Float mapDefaultLatitude = Utils.DEFAULT_MAP_LATITUDE
    Float mapDefaultLongitude = Utils.DEFAULT_MAP_LONGITUDE
    Integer mapZoom = Utils.DEFAULT_MAP_ZOOM
    String mapBaseLayer = Utils.DEFAULT_MAP_BASE_LAYER
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
    boolean privateCollection = false
    Glossary glossary
    String keybaseProjectId
    String keybaseKeyId
    String aboutHtml
    String citationHtml
    String copyrightText
    String footerText
    String email
    String facebook
    String twitter
    List<String> excludeRanksFromMap
    List<SupportingOpus> supportingOpuses
    List<SupportingOpus> sharingDataWith
    boolean autoApproveShareRequests = true
    boolean keepImagesPrivate = false

    static hasMany = [additionalOccurrenceResources: OccurrenceResource, authorities: Authority]
    static embedded = ['supportingOpuses', 'sharingDataWith']

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
        citationHtml nullable: true
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
