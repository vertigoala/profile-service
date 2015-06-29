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
                List profiles = Profile.findAllByOpusAndDraftIsNotNull(opus).collect {
                    [profileId: it.uuid, scientificName: it.scientificName, draftDate: it.draft.draftDate, createdBy: it.draft.createdBy]
                }.sort { it.scientificName }

                Map report = [
                        recordCount: profiles.size(),
                        records    : profiles
                ]


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
                int max = params.max && params.max != "null" ? params.max as int : -1
                int startFrom = params.offset ? params.offset as int : 0

                int count = -1
                if (max > -1) {
                    count = Profile.withCriteria {
                        eq "opus", opus

                        or {
                            isNull "matchedName"
                            neProperty("fullName", "matchedName.fullName")
                        }
                    }.size()
                }

                def result = Profile.withCriteria {
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

                Map report = [recordCount: count > -1 ? count : result.size(),
                              records    : result.collect {
                                  [
                                          profileId  : it.uuid,
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
