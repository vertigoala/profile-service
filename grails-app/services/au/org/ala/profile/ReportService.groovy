package au.org.ala.profile

class ReportService {

    Map draftProfiles(String opusId) {
        Opus opus = Opus.findByUuid(opusId)

        List profiles = Profile.findAllByOpusAndDraftIsNotNull(opus).collect {
            [profileId: it.uuid, scientificName: it.scientificName, draftDate: it.draft.draftDate, createdBy: it.draft.createdBy]
        }.sort { it.scientificName }

        [recordCount: profiles.size(), records: profiles]
    }

    Map archivedProfiles(String opusId) {
        Opus opus = Opus.findByUuid(opusId)

        List profiles = Profile.findAllByOpusAndArchivedDateIsNotNull(opus).collect {
            [profileId: it.uuid, scientificName: it.archivedWithName, archivedDate: it.archivedDate, archivedBy: it.archivedBy]
        }.sort { it.scientificName }

        [recordCount: profiles.size(), records: profiles]
    }

    Map mismatchedNames(String opusId, int max, int startFrom) {
        Opus opus = Opus.findByUuid(opusId)

        int count = -1
        if (max > -1) {
            count = Profile.withCriteria {
                eq "opus", opus

                or {
                    isNull "matchedName"
                    neProperty("fullName", "matchedName.fullName")
                    isNull "nslNameIdentifier"
                }
            }.size()
        }

        def result = Profile.withCriteria {
            eq "opus", opus

            or {
                isNull "matchedName"
                neProperty("fullName", "matchedName.fullName")
                isNull "nslNameIdentifier"
            }

            order "scientificName"

            if (max > 0) {
                maxResults max
                offset startFrom
            }
        }

        [recordCount: count > -1 ? count : result.size(),
         records    : result.collect {
             [
                     profileId  : it.uuid,
                     profileName: [scientificName: it.scientificName,
                                   fullName      : it.fullName,
                                   nameAuthor    : it.nameAuthor],
                     matchedName: it.matchedName ? [scientificName: it.matchedName.scientificName,
                                                    fullName      : it.matchedName.fullName,
                                                    nameAuthor    : it.matchedName.nameAuthor,
                                                    guid          : it.matchedName.guid] : [:],
                     nslNameId  : it.nslNameIdentifier
             ]
         }]
    }

    Map recentUpdates(String opusId, Date from, Date to, int max, int startFrom) {
        Opus opus = Opus.findByUuid(opusId)

        int count = -1
        if (max > -1) {
            count = Profile.withCriteria {
                eq('opus', opus)
                and {
                    le('lastUpdated', to)
                    ge('lastUpdated', from)
                }
                order('lastUpdated', "desc")
            }.size()
        }

        //get profiles updated or profiles with the given ids
        List profiles = Profile.withCriteria {
            eq('opus', opus)
            and {
                le('lastUpdated', to)
                ge('lastUpdated', from)
            }
            order('lastUpdated', "desc")

            if (max > 0) {
                maxResults max
                offset startFrom
            }
        }.collect {
            [profileId: it.uuid, scientificName: it.scientificName, lastUpdated: it.lastUpdated, editor: it.lastUpdatedBy]
        }

        [recordCount: count > 0 ? count : profiles.size(), records: profiles]
    }
}
