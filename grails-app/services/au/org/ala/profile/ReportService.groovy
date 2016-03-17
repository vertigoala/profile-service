package au.org.ala.profile

import au.org.ala.profile.util.Utils
import grails.gorm.PagedResultList

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

    Map mismatchedNames(String opusId, int max, int startFrom, boolean countOnly) {
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

                isNull "archivedDate"
            }.size()
        }

        if (countOnly) {
            [recordCount: count > 0 ? count : 0]
        }
        else {
            def result = Profile.withCriteria {
                eq "opus", opus

                or {
                    isNull "matchedName"
                    neProperty("fullName", "matchedName.fullName")
                    isNull "nslNameIdentifier"
                }

                isNull "archivedDate"

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
    }

    Map recentUpdates(String opusId, Date from, Date to, int max, int startFrom,
                      boolean countOnly) {
        Opus opus = Opus.findByUuid(opusId)

        int count = -1
        if (max > -1) {
            count = Profile.withCriteria {
                eq('opus', opus)
                and {
                    le('lastUpdated', to)
                    ge('lastUpdated', from)
                }
                isNull "archivedDate"

                order('lastUpdated', "desc")
            }.size()
        }

        if (countOnly) {
            [recordCount: count > 0 ? count : 0]
        }
        else {
            //get profiles updated or profiles with the given ids
            List profiles = Profile.withCriteria {
                eq('opus', opus)
                and {
                    le('lastUpdated', to)
                    ge('lastUpdated', from)
                }
                isNull "archivedDate"

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

    Map recentComments(Opus opus, Date from, Date to, int max, int startFrom, boolean countOnly) {
        final profiles = Profile.withCriteria {
            eq('opus', opus)
            projections {
                property('uuid')
                property('scientificName')
            }
        }
        final profileUuids = profiles*.get(0)

        final commentCriteriaClosure = {
            'in'('profileUuid', profileUuids)
            between('lastUpdated', from, to)
            order('lastUpdated', "desc")
        }

        final commentCriteria = Comment.createCriteria()

        final count
        if (countOnly) {
            count = commentCriteria.count(commentCriteriaClosure)
            [recordCount: count > 0 ? count : 0]
        } else {
            final profileMap = profiles.collectEntries { [(it[0]): it[1]] }

            final comments = (max > 0 ?
                    commentCriteria.list(max: max, offset: startFrom, commentCriteriaClosure) :
                    commentCriteria.list(commentCriteriaClosure))
            final records = comments.collect { [
                    comment       : it.text,
                    plainComment  : Utils.cleanupText(it.text),
                    scientificName: profileMap[it.profileUuid],
                    lastUpdated   : it.lastUpdated,
                    editor        : it.author?.name
            ] }

            count = comments instanceof PagedResultList ? comments.totalCount : comments.size()

            [recordCount: count, records: records]
        }
    }
}
