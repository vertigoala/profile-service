package au.org.ala.profile

import grails.converters.JSON

class ReportController extends BaseController {
    def draftProfiles() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for ${params.opusId}"
            } else {
                List report = Profile.findAllByOpusAndDraftIsNotNull(opus).collect {
                    [profileId: it.uuid, scientificName: it.scientificName]
                }

                render report as JSON
            }
        }
    }

    def mismatchedNames() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No opus found for ${params.opusId}"
            } else {
                int max = params.max ? params.max as int : -1
                int startFrom = params.offset ? params.offset as int : 0

                List<Profile> result = Profile.withCriteria {
                    eq "opus", opus

                    or {
                        isNull "matchedName"
                        neProperty("fullName", "matchedName.fullName")
                    }

                    order "scientificName"

                    if (max > 0) {
                        maxResults max
                        offset startFrom
                    }
                }

                Map report = [mismatchedRecords: result.size(),
                              records          : result.collect {
                                  [
                                          profileId: it.uuid,
                                          profileName: [scientificName: it.scientificName,
                                                        fullName      : it.fullName,
                                                        nameAuthor    : it.nameAuthor],
                                          matchedName: it.matchedName ? [scientificName: it.matchedName.scientificName,
                                                                         fullName      : it.matchedName.fullName,
                                                                         nameAuthor    : it.matchedName.nameAuthor,
                                                                         guid          : it.matchedName.guid] : [:]
                                  ]
                              }]

                render report as JSON
            }
        }
    }
}
