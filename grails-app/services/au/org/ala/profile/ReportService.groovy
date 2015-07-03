package au.org.ala.profile

class ReportService {

    /**
     * get the most recent change between two dates for an opus.
     * used to generate reports
     * @param from
     * @param to
     * @param opus
     * @return
     */
    Map mostRecentChange(Date from, Date to, Opus opus, int max, int startFrom) {

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

        Map report = [
                recordCount: count > 0 ? count:profiles.size(),
                records    : profiles
        ]
    }
}
