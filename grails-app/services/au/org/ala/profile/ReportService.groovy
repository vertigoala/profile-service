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
    Map mostRecentChange(Date from, Date to, Opus opus) {

        // check if attributes are updated since attribute update does not change
        // profile's lastUpdated attribute
        List attributes = Attribute.withCriteria {
            ge('lastUpdated', from)
            le('lastUpdated', to)
            projections{
                distinct('profile')
            }
        };

        //get the identifier
        List profileIds = [];
        attributes.each {
            profileIds.push(it.uuid);
        };

        //get profiles updated or profiles with the given ids
        List profiles = Profile.withCriteria{
            eq('opus', opus)
            and {
                or{
                    and{
                        le('lastUpdated', to)
                        ge('lastUpdated', from)
                    }
                    'in'("uuid",profileIds )
                }

            }
            order('lastUpdated', "desc")
        }.collect {
            [profileId: it.uuid, scientificName: it.scientificName, lastUpdated: it.lastUpdated]
        }

        Map report = [
                recordCount: profiles.size(),
                records    : profiles
        ]
    }
}
