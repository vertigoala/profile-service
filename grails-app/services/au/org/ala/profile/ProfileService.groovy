package au.org.ala.profile

import grails.transaction.Transactional

@Transactional
class ProfileService {

    boolean createOpus(json) {
        Opus opus = new Opus(json)

        opus.save(flush: true)

        boolean saved

        if (opus.errors.allErrors.size() > 0) {
            log.error("Failed to create opus with id")
            opus.errors.each { log.error(it) }
            saved = false
        } else {
            log.info("Opus ${opus.uuid} created")
            saved = true
        }

        saved
    }

    boolean updateOpus(opusId, json) {
        Opus opus = Opus.findByUuid(opusId)

        if (json.title && json.title != opus.title) {
            opus.title = json.title
        }
        if (json.imageSources && json.imageSources != opus.imageSources) {
            log.debug("Image sources:")
            opus.imageSources.each {log.debug("'${it}'")}

            opus.imageSources.clear()
            opus.imageSources.addAll(json.imageSources)
        }
        if (json.recordSources && json.recordSources != opus.recordSources) {
            log.debug("Record sources:")
            opus.recordSources.each {log.debug("'${it}'")}

            opus.recordSources.clear()
            opus.recordSources.addAll(json.recordSources)
        }
        if (json.logoUrl && json.logoUrl != opus.logUrl) {
            opus.logoUrl = json.logoUrl
        }
        if (json.bannerUrl && json.bannerUrl != opus.bannerUrl) {
            opus.bannerUrl = json.bannerUrl
        }
        if (json.mapAttribution && json.mapAttribution != opus.mapAttribution) {
            opus.mapAttribution = json.mapAttribution
        }
        if (json.mapPointColour && json.mapPointColour != opus.mapPointColour) {
            opus.mapPointColour = json.mapPointColour;
        }
        if (json.mapDefaultLatitude && json.mapDefaultLatitude != opus.mapDefaultLatitude) {
            opus.mapDefaultLatitude = json.mapDefaultLatitude as Float
        }
        if (json.mapDefaultLongitude && json.mapDefaultLongitude != opus.mapDefaultLongitude) {
            opus.mapDefaultLongitude = json.mapDefaultLongitude as Float
        }
        if (json.mapZoom && json.mapZoom != opus.mapZoom) {
            opus.mapZoom = json.mapZoom as int
        }
        if (json.mapBaseLayer && json.mapBaseLayer != opus.mapBaseLayer) {
            opus.mapBaseLayer = json.mapBaseLayer
        }
        if (json.biocacheUrl && json.biocacheUrl != opus.biocacheUrl) {
            opus.biocacheUrl = json.biocacheUrl
        }
        if (json.biocacheName && json.biocacheName != opus.biocacheName) {
            opus.biocacheName = json.biocacheName
        }
        if (json.enablePhyloUpload && json.enablePhyloUpload != opus.enablePhyloUpload) {
            opus.enablePhyloUpload = json.enablePhyloUpload as boolean
        }
        if (json.enableOccurrenceUpload && json.enableOccurrenceUpload != opus.enableOccurrenceUpload) {
            opus.enableOccurrenceUpload = json.enableOccurrenceUpload as boolean
        }
        if (json.enableTaxaUpload && json.enableTaxaUpload != opus.enableTaxaUpload) {
            opus.enableTaxaUpload = json.enableTaxaUpload as boolean
        }
        if (json.enableKeyUpload && json.enableKeyUpload != opus.enableKeyUpload) {
            opus.enableKeyUpload = json.enableKeyUpload as boolean
        }

        opus.save(flush: true)

        boolean saved

        if (opus.errors.allErrors.size() > 0) {
            log.error("Failed to update opus with id ${opusId}")
            opus.errors.each { log.error(it) }
            saved = false
        } else {
            log.info("Opus ${opusId} updated")
            saved = true
        }

        saved
    }



    boolean deleteAttribute(String attributeId, String profileId) {

        Attribute attr = Attribute.findByUuid(attributeId)
        Profile profile = Profile.findByUuid(profileId)

        log.debug("Deleting attribute ${attr?.title} from profile ${profile?.scientificName}...")

        boolean deleted = false;
        if (attr && profile) {
            log.debug("Removing attribute from Profile...")
            profile.removeFromAttributes(attr)
            profile.save(flush: true)

            attr.delete(flush: true)

            if (attr.errors.allErrors.size() > 0) {
                log.error("Failed to delete attribute with id ${attributeId}")
                attr.errors.each { log.error(it) }
                deleted = false
            } else {
                log.info("Attribute ${attributeId} deleted")
                deleted = true
            }
        } else {
            log.error("Failed to find matching attribute for id ${attributeId} and/or profile for id ${profileId}")
        }

        deleted
    }

}
