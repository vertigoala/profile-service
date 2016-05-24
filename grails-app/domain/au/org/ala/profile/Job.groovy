package au.org.ala.profile

import au.org.ala.profile.util.JobType
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Job {

    String jobId
    JobType jobType
    int attempt
    Map params
    String error
    Date dateCreated
    Date lastUpdated
    String userEmail

    static constraints = {
        error nullable: true
        userEmail nullable: true
    }
}
