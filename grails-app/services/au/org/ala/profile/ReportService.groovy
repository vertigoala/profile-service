package au.org.ala.profile

class ReportService {

    Map mostRecentChange(Date from, Date to, Opus opus) {

        List attributes = Attribute.withCriteria {
            ge('lastUpdated', from)
            le('lastUpdated', to)
            projections{
                distinct('profile')
            }
        };
        List profileIds = [];
        attributes.each {
            profileIds.push(it.uuid);
        };

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
