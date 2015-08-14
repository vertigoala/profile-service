package au.org.ala.profile.marshaller

import au.org.ala.profile.Opus
import au.org.ala.profile.SupportingOpus
import au.org.ala.profile.util.ShareRequestStatus
import au.org.ala.profile.util.Utils
import grails.converters.JSON

class OpusMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Opus) { Opus opus ->
            return [
                    uuid                       : opus.uuid,
                    dataResourceUid            : opus.dataResourceUid,
                    title                      : opus.title,
                    shortName                  : opus.shortName,
                    description                : opus.description,
                    imageSources               : opus.imageSources ?: [],
                    recordSources              : opus.recordSources ?: [],
                    approvedLists              : opus.approvedLists ?: [],
                    bioStatusLists             : opus.bioStatusLists ?: [],
                    logoUrl                    : opus.logoUrl,
                    bannerUrl                  : opus.bannerUrl,
                    keybaseProjectId           : opus.keybaseProjectId,
                    keybaseKeyId               : opus.keybaseKeyId,
                    thumbnailUrl               : opus.thumbnailUrl,
                    attributeVocabUuid         : opus.attributeVocabUuid,
                    authorshipVocabUuid        : opus.authorshipVocabUuid,
                    glossaryUuid               : opus.glossary?.uuid,
                    enablePhyloUpload          : opus.enablePhyloUpload != null ? opus.enableKeyUpload : true,
                    enableOccurrenceUpload     : opus.enableOccurrenceUpload != null ? opus.enableKeyUpload : true,
                    enableTaxaUpload           : opus.enableTaxaUpload != null ? opus.enableKeyUpload : true,
                    enableKeyUpload            : opus.enableKeyUpload != null ? opus.enableKeyUpload : true,
                    privateCollection          : opus.privateCollection != null ? opus.privateCollection : false,
                    mapAttribution             : opus.mapAttribution ?: 'Atlas',
                    mapPointColour             : opus.mapPointColour ?: "FF9900",
                    mapDefaultLatitude         : opus.mapDefaultLatitude ?: Utils.DEFAULT_MAP_LATITUDE,
                    mapDefaultLongitude        : opus.mapDefaultLongitude ?: Utils.DEFAULT_MAP_LONGITUDE,
                    mapZoom                    : opus.mapZoom ?: Utils.DEFAULT_MAP_ZOOM,
                    mapBaseLayer               : opus.mapBaseLayer ?: Utils.DEFAULT_MAP_BASE_LAYER,
                    biocacheUrl                : opus.biocacheUrl,
                    biocacheName               : opus.biocacheName ?: 'Atlas',
                    supportingOpuses           : marshalSupportingOpuses(opus.supportingOpuses?.findAll {
                        it.requestStatus == ShareRequestStatus.ACCEPTED
                    }),
                    requestedSupportingOpuses  : marshalSupportingOpuses(opus.supportingOpuses?.findAll {
                        it.requestStatus != ShareRequestStatus.ACCEPTED
                    }),
                    sharingDataWith            : marshalSupportingOpuses(opus.sharingDataWith),
                    autoApproveShareRequests   : opus.autoApproveShareRequests,
                    allowCopyFromLinkedOpus    : opus.allowCopyFromLinkedOpus != null ? opus.allowCopyFromLinkedOpus : false,
                    showLinkedOpusAttributes   : opus.showLinkedOpusAttributes != null ? opus.showLinkedOpusAttributes : false,
                    allowFineGrainedAttribution: opus.allowFineGrainedAttribution != null ? opus.allowFineGrainedAttribution : true,
                    authorities                : opus.authorities?.collect {
                        [userId: it.user.userId, name: it.user.name, role: it.role.toString(), notes: it.notes]
                    },
                    copyrightText              : opus.copyrightText,
                    footerText                 : opus.footerText,
                    contact                    : [twitter : opus.twitter,
                                                  email   : opus.email,
                                                  facebook: opus.facebook],
                    hasAboutPage               : opus.aboutHtml != null,
                    excludeRanksFromMap        : opus.excludeRanksFromMap ?: [],
                    profileCount               : opus.profileCount,
                    citationHtml               : opus.citationHtml
            ]
        }
    }

    def static marshalSupportingOpuses(List opuses) {
        opuses ?
                opuses.collect {
                    [uuid: it.uuid, title: it.title, requestStatus: it.requestStatus.toString()]
                } : []
    }
}
