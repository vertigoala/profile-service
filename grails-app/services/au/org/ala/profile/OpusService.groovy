package au.org.ala.profile

import grails.transaction.Transactional

@Transactional
class OpusService extends BaseDataAccessService {

    Opus createOpus(json) {
        log.debug("Creating new opus record")
        Opus opus = new Opus(json)

        save opus
    }

    Opus updateOpus(opusId, json) {
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
        if (json.logoUrl && json.logoUrl != opus.logoUrl) {
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

        save opus
    }

    boolean deleteOpus(String opusId) {
        Opus opus = Opus.findByUuid(opusId);

        boolean deleted = false

        if (opus) {
            opus.delete(flush: true)
            deleted = true
        }

        deleted
    }
}
