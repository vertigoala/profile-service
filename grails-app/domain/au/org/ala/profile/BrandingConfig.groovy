package au.org.ala.profile

/**
 * Embedded entity to encapsulate the configuration items that control the branding on the UI (banners, logos, etc)
 */
class BrandingConfig {
    static final int DEFAULT_OPUS_BANNER_HEIGHT_PX = 300
    static final int DEFAULT_PROFILE_BANNER_HEIGHT_PX = 100

    String opusBannerUrl // banner image for non-profile pages
    int opusBannerHeight = DEFAULT_OPUS_BANNER_HEIGHT_PX

    String profileBannerUrl // banner image for profile pages
    int profileBannerHeight = DEFAULT_PROFILE_BANNER_HEIGHT_PX

    List<Logo> logos = []
    String thumbnailUrl

    String colourTheme

    static constraints = {
        opusBannerUrl nullable: true
        profileBannerUrl nullable: true
        thumbnailUrl nullable: true
        colourTheme nullable: true
    }

    static mapping = {
        opusBannerHeight defaultValue: DEFAULT_OPUS_BANNER_HEIGHT_PX
        profileBannerHeight defaultValue: DEFAULT_PROFILE_BANNER_HEIGHT_PX
    }

    static embedded = ["logos"]
}
