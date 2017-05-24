package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml
import au.org.ala.profile.util.ImageOption
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.persistence.Transient

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

    String masterListUid
    List<String> approvedLists
    List<String> featureLists
    String featureListSectionName

    BrandingConfig brandingConfig
    ProfileLayoutConfig profileLayoutConfig
    MapConfig mapConfig
    DataResourceConfig dataResourceConfig
    OpusLayoutConfig opusLayoutConfig

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
    @SanitizedHtml
    String aboutHtml
    @SanitizedHtml
    String citationHtml
    String citationProfile
    String copyrightText
    String footerText
    String email
    String facebook
    String twitter
    List<SupportingOpus> supportingOpuses
    List<SupportingOpus> sharingDataWith
    List<Attachment> attachments
    boolean autoApproveShareRequests = true
    boolean keepImagesPrivate = false
    boolean usePrivateRecordData = false
    ImageOption approvedImageOption = ImageOption.INCLUDE

    String accessToken

    boolean autoDraftProfiles = false // automatically lock profiles for draft when they are created

    @Transient
    int profileCount

    List<String> additionalStatuses = ['In Review', 'Complete']

    static hasMany = [additionalOccurrenceResources: OccurrenceResource, authorities: Authority, tags: Tag]
    static embedded = ['supportingOpuses', 'sharingDataWith', 'attachments', 'brandingConfig', 'mapConfig', 'profileLayoutConfig', 'dataResourceConfig', 'opusLayoutConfig']

    static constraints = {
        shortName nullable: true
        description nullable: true
        masterListUid nullable: true
        brandingConfig nullable: true
        profileLayoutConfig nullable: true
        mapConfig nullable: true
        dataResourceConfig nullable: true
        opusLayoutConfig nullable: true
        attributeVocabUuid nullable: true
        authorshipVocabUuid nullable: true
        enablePhyloUpload nullable: true
        enableOccurrenceUpload nullable: true
        enableTaxaUpload nullable: true
        enableKeyUpload nullable: true
        keybaseProjectId nullable: true
        keybaseKeyId nullable: true
        aboutHtml nullable: true
        citationHtml nullable: true
        citationProfile nullable: true, maxSize: 500
        copyrightText nullable: true
        footerText nullable: true
        email nullable: true
        facebook nullable: true
        twitter nullable: true
        featureListSectionName nullable: true
        accessToken nullable: true
    }

    def beforeValidate() {
        if (uuid == null) {
            //mint an UUID
            uuid = UUID.randomUUID().toString()
        }
    }

    static mapping = {
        glossary cascade: "all-delete-orphan"
        authorities cascade: "all-delete-orphan"
        shortName index: true
        uuid index: true
    }
}
