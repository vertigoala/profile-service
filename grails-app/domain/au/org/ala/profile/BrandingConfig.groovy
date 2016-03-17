package au.org.ala.profile

/**
 * Embedded entity to encapsulate the configuration items that control the branding on the UI (banners, logos, etc)
 */
class BrandingConfig {
    static final int DEFAULT_LANDING_BANNER_HEIGHT_PX = 300
    static final int DEFAULT_OTHER_BANNER_HEIGHT_PX = 100

    String opusBannerUrl // banner image for non-profile pages
    int opusBannerHeight = DEFAULT_LANDING_BANNER_HEIGHT_PX

    String profileBannerUrl // banner image for profile pages
    int profileBannerHeight = DEFAULT_OTHER_BANNER_HEIGHT_PX

    String logoUrl
    String thumbnailUrl

    static constraints = {
        opusBannerHeight nullable: true
        opusBannerUrl nullable: true
        profileBannerHeight nullable: true
        profileBannerUrl nullable: true
        logoUrl nullable: true
        thumbnailUrl nullable: true
    }
}
