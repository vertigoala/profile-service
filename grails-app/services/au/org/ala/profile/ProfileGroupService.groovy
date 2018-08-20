package au.org.ala.profile

import org.springframework.transaction.annotation.Transactional

@Transactional
/**
 * Grails Service for ProfileGroup database operations
 */
class ProfileGroupService extends BaseDataAccessService {

    /**
     * Create a profile group by given opus and map of values
     * @param opusId uuid of the opus
     * @param json map containing group key-value pairs
     * @return the profile group
     */
    ProfileGroup createGroup(String opusId, Map json) {
        checkArgument(opusId)
        checkArgument(json)

        Opus opus = Opus.findByUuid(opusId)
        checkState(opus)

        ProfileGroup group = new ProfileGroup(json)
        group.opus = opus

        boolean success = save group
        if (!success) {
            group = null
        }

        group
    }

    def listProfiles(ProfileGroup group) {

    }

    ProfileGroup updateGroup(String groupId, Map json) {

    }

    /**
     * Delete profile group and its profiles
     * @param groupId uuid of the group
     * @return status of the deletion
     */
    boolean deleteGroup(String groupId) {
        ProfileGroup group = ProfileGroup.findByUuid(groupId)

        if (group) {
            List profiles = Profile.findAllByGroup(group)
            Profile.deleteAll(profiles)
        }

        delete group
    }

    /**
     * Throws an IllegalArgumentException if the provided argument is null or empty
     *
     * @param arg The argument to check
     * @param an optional message to include in the IllegalArgumentException
     */
    void checkArgument(arg, message = "") {
        if (arg == null || (arg.getMetaClass() && arg.getMetaClass().respondsTo(arg, "isEmpty") && arg.isEmpty())) {
            throw new IllegalArgumentException(message)
        }
    }

    /**
     * Throws an IllegalStateException if the provided state evaluates to false with
     *
     * @param state The state to check
     * @param message an optional message to include in the IllegalArgumentException
     */
    void checkState(state, message = "") {
        if (!state) {
            throw new IllegalStateException(message)
        }
    }
}
