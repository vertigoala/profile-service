package au.org.ala.profile

import au.org.ala.profile.util.DataResourceOption
import au.org.ala.ws.controller.BasicWSController
import com.google.common.base.Stopwatch

import static au.org.ala.profile.util.Utils.isUuid
import static au.org.ala.profile.util.Utils.enc

class BaseController extends BasicWSController {

    ProfileService profileService

    Profile getProfile() {
        Stopwatch sw = new Stopwatch().start()

        Profile profile

        if (isUuid(params.profileId)) {
            profile = Profile.findByUuid(params.profileId)
            log.debug("getProfile() - Get profile by UUID ${params.profileId}: $sw")
            sw.reset().start()
        } else {
            Opus opus = getOpus()
            profile = Profile.findByOpusAndScientificNameIlike(opus, params.profileId)
            log.debug("getProfile() - Get profile by opus ${opus.uuid} and sci name ${params.profileId}: $sw")
            sw.reset().start()

            // names can be changed, so if there is no profile with the name, check for a draft with that name, but only if the 'latest' flag is true
            if (!profile && params.latest?.toBoolean()) {
                List matches = Profile.withCriteria {
                    eq "opus", opus
                    ilike "draft.scientificName", params.profileId
                }
                profile = matches.isEmpty() ? null : matches.first()

                log.debug("getProfile() - Get profile by with changed name: $sw")
                sw.reset().start()
            }
        }

        if (profile && profile.classification && params.boolean('countChildren', false)) {
            def classifications = profile.draft && params.latest == "true" ? profile.draft.classification : profile.classification
            classifications.each { cl ->
                cl.childCount = Profile.withCriteria {
                    eq "opus", profile.opus
                    isNull "archivedDate"
                    ne "uuid", profile.uuid

                    "classification" {
                        eq "rank", "${cl.rank?.toLowerCase()}"
                        ilike "name", "${cl.name}"
                    }

                    projections {
                        count()
                    }
                }[0]

                Profile relatedProfile = Profile.findByGuidAndOpusAndArchivedDateIsNull(cl.guid, opus)
                if (!relatedProfile) {
                    relatedProfile = Profile.findByScientificNameAndOpusAndArchivedDateIsNull(cl.name, opus)
                }
                cl.profileId = relatedProfile?.uuid
                cl.profileName = relatedProfile?.scientificName
            }

            log.debug("getProfile() - Get classification childCounts, profile ids and profileNames: $sw")
            sw.reset().start()
        }

        // if the profile has no specific occurrence query then we just set it to the default for the collection,
        // which limits the query to the LSID (or name if there is no LSID) and the selected data resources
        if (!profile.occurrenceQuery) {
            String query = createOccurrenceQuery(profile)
            profile.occurrenceQuery = query
            if (profile.draft) {
                profile.draft.occurrenceQuery = query
            }
            log.debug("getProfile() - createOccurenceQuery: $sw")
            sw.reset().start()
        }

        profile
    }

    private String createOccurrenceQuery(Profile profile) {
        Opus opus = profile.opus

        String result = ""

        if (profile && opus) {
            String query = ""

            if (profile.guid && profile.guid != "null") {
                query += "${"lsid:${profile.guid}"}"
            } else {
                query += profile.scientificName
            }

            String occurrenceQuery = query

            if (opus.usePrivateRecordData) {
                DataResourceConfig config = opus.dataResourceConfig
                if (config?.privateRecordSources) {
                    occurrenceQuery = "${query} AND (data_resource_uid:${config.privateRecordSources?.join(" OR data_resource_uid:")})"
                }
            } else if (opus.dataResourceConfig) {
                DataResourceConfig config = opus.dataResourceConfig
                switch (config.recordResourceOption) {
                    case DataResourceOption.ALL:
                        occurrenceQuery = query
                        break
                    case DataResourceOption.NONE:
                        occurrenceQuery = "${query} AND data_resource_uid:${opus.dataResourceUid}"
                        break
                    case DataResourceOption.HUBS:
                        occurrenceQuery = "${query} AND (data_resource_uid:${opus.dataResourceUid} OR data_hub_uid:${config.recordSources?.join(" OR data_hub_uid:")})"
                        break
                    case DataResourceOption.RESOURCES:
                        occurrenceQuery = "${query} AND (data_resource_uid:${opus.dataResourceUid} OR data_resource_uid:${config.recordSources?.join(" OR data_resource_uid:")})"
                        break
                }
            }

            result = "q=${enc(occurrenceQuery)}"
        }

        result
    }

    Opus getOpus() {
        Stopwatch sw = new Stopwatch().start()
        Opus opus
        if (isUuid(params.opusId)) {
            opus = Opus.findByUuid(params.opusId)
            log.debug("getOpus() - Get opus by UUID ${params.opusId}: $sw")
        } else {
            opus = Opus.findByShortName(params.opusId.toLowerCase())
            log.debug("getOpus() - Get opus by short name ${params.opusId}: $sw")
        }
        opus
    }
}
