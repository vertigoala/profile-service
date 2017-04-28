package au.org.ala.profile

import au.org.ala.profile.sanitizer.SanitizedHtml

class OpusLayoutConfig {
    @SanitizedHtml
    String explanatoryText
    List<Image> images = []
    Integer duration = 5000
    @SanitizedHtml
    String updatesSection
    String helpTextSearch
    String helpTextIdentify
    String helpTextBrowse
    String helpTextDocuments
    static constraints = {
        explanatoryText nullable: true
        updatesSection nullable: true
        helpTextSearch nullable: true
        helpTextIdentify nullable: true
        helpTextBrowse nullable: true
        helpTextDocuments nullable: true
    }

    static embedded = ['images']
}
