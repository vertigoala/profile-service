package au.org.ala.profile

import au.org.ala.profile.util.DataResourceOption
import au.org.ala.ws.controller.BasicWSController

import static au.org.ala.profile.util.Utils.isUuid
import static au.org.ala.profile.util.Utils.enc

class BaseController extends BasicWSController {

    ProfileService profileService

    Profile getProfile() {
        Profile profile

        if (isUuid(params.profileId)) {
            profile = Profile.findByUuid(params.profileId)
        } else {
            Opus opus = getOpus()
            profile = Profile.findByOpusAndScientificNameIlike(opus, params.profileId)

            // names can be changed, so if there is no profile with the name, check for a draft with that name, but only if the 'latest' flag is true
            if (!profile && params.latest?.toBoolean()) {
                List matches = Profile.withCriteria {
                    eq "opus", opus
                    ilike "draft.scientificName", params.profileId
                }
                profile = matches.isEmpty() ? null : matches.first()
            }
        }

        if (profile && profile.classification) {
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
        }

        // if the profile has no specific occurrence query then we just set it to the default for the collection,
        // which limits the query to the LSID (or name if there is no LSID) and the selected data resources
        if (!profile.occurrenceQuery) {
            String query = createOccurrenceQuery(profile)
            profile.occurrenceQuery = query
            if (profile.draft) {
                profile.draft.occurrenceQuery = query
            }
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

            if (opus.dataResourceConfig) {
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
        Opus opus
        if (isUuid(params.opusId)) {
            opus = Opus.findByUuid(params.opusId)
        } else {
            opus = Opus.findByShortName(params.opusId.toLowerCase())
        }
        opus
    }
}
