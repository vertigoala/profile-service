package au.org.ala.profile.marshaller

import au.org.ala.profile.Opus
import grails.converters.JSON

class OpusMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Opus) { Opus opus ->
            return [
                    uuid                    : "${opus.uuid}",
                    dataResourceUid         : "${opus.dataResourceUid}",
                    title                   : "${opus.title}",
                    imageSources            : opus.imageSources ?: [],
                    recordSources           : opus.recordSources ?: [],
                    logoUrl                 : opus.logoUrl,
                    bannerUrl               : opus.bannerUrl,
                    thumbnailUrl            : opus.thumbnailUrl,
                    attributeVocabUuid      : opus.attributeVocabUuid,
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
                    admins                  : opus.admins?.collect { [userId: it.userId, name: it.name] },
                    editors                 : opus.editors?.collect { [userId: it.userId, name: it.name] }
            ]
        }
    }

    static def collectSupportingOpuses(opus) {
        opus.supportingOpuses ? opus.supportingOpuses.each { [opusId: it.uuid, title: it.title] } : []
    }
}
