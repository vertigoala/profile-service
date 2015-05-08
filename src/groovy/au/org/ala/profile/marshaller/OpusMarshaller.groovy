package au.org.ala.profile.marshaller

import au.org.ala.profile.Opus
import grails.converters.JSON

class OpusMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Opus) { Opus opus ->
            return [
                    uuid                    : opus.uuid,
                    dataResourceUid         : opus.dataResourceUid,
                    title                   : opus.title,
                    shortName               : opus.shortName,
                    imageSources            : opus.imageSources ?: [],
                    recordSources           : opus.recordSources ?: [],
                    approvedLists           : opus.approvedLists ?: [],
                    logoUrl                 : opus.logoUrl,
                    bannerUrl               : opus.bannerUrl,
                    thumbnailUrl            : opus.thumbnailUrl,
                    attributeVocabUuid      : opus.attributeVocabUuid,
                    glossaryUuid            : opus.glossary?.uuid,
                    enablePhyloUpload       : opus.enablePhyloUpload != null ? opus.enableKeyUpload : true,
                    enableOccurrenceUpload  : opus.enableOccurrenceUpload != null ? opus.enableKeyUpload : true,
                    enableTaxaUpload        : opus.enableTaxaUpload != null ? opus.enableKeyUpload : true,
                    enableKeyUpload         : opus.enableKeyUpload != null ? opus.enableKeyUpload : true,
                    mapAttribution          : opus.mapAttribution ?: 'Atlas',
                    mapPointColour          : opus.mapPointColour ?: "FF9900",
                    mapDefaultLatitude      : opus.mapDefaultLatitude ?: -23.6,
                    mapDefaultLongitude     : opus.mapDefaultLongitude ?: 133.6,
                    mapZoom                 : opus.mapZoom ?: 3,
                    mapBaseLayer            : opus.mapBaseLayer ?: "https://{s}.tiles.mapbox.com/v3/{id}/{z}/{x}/{y}.png",
                    biocacheUrl             : opus.biocacheUrl,
                    biocacheName            : opus.biocacheName ?: 'Atlas',
                    supportingOpuses        : collectSupportingOpuses(opus),
                    allowCopyFromLinkedOpus : opus.allowCopyFromLinkedOpus != null ? opus.allowCopyFromLinkedOpus : false,
                    showLinkedOpusAttributes: opus.showLinkedOpusAttributes != null ? opus.showLinkedOpusAttributes : false,
                    authorities             : opus.authorities?.collect {[userId: it.user.userId, name: it.user.name, role: it.role.toString(), notes: it.notes]},
                    profileCount            : opus.profileCount
            ]
        }
    }

    static def collectSupportingOpuses(opus) {
        opus.supportingOpuses ? opus.supportingOpuses.each { [opusId: it.uuid, title: it.title] } : []
    }
}
