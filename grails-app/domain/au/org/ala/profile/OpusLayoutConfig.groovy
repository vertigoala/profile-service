package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml

class OpusLayoutConfig {
    String opusLogoUrl
    @SanitizedHtml
    String explanatoryText
    List<Image> images = []
    Integer duration = 5000
    @SanitizedHtml
    String updatesSection
    Integer gradient
    Double gradientWidth
    String helpTextSearch
    String helpTextIdentify
    String helpTextFilter
    String helpTextBrowse
    String helpTextDocuments
    @SanitizedHtml
    String bannerOverlayText
    static constraints = {
        opusLogoUrl nullable: true
        explanatoryText nullable: true
        updatesSection nullable: true
        helpTextSearch nullable: true
        helpTextIdentify nullable: true
        helpTextFilter nullable: true
        helpTextBrowse nullable: true
        helpTextDocuments nullable: true
        gradient nullable: true
        gradientWidth nullable: true, min: 0.0d, max: 100.0d
        bannerOverlayText nullable: true
    }

    static embedded = ['images']
}
