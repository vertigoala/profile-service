package au.org.ala.web

/**
 * Created with IntelliJ IDEA.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
class UserDetails {

    public static final String REQUEST_USER_DETAILS_KEY = 'request.user.details'

    String displayName // full name
    String userName    // email
    String userId      // String id

    @Override
    public String toString() {
        "[ userId: ${userId}, userName: ${userName}, displayName: ${displayName} ]"
    }
}
