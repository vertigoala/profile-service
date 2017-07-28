package au.org.ala.profile

import au.org.ala.web.AuthService

class CommentService extends BaseDataAccessService {

    AuthService authService

    Comment createComment(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        checkState Profile.findByUuid(profileId)

        Comment comment = new Comment(json)
        comment.profileUuid = profileId

        comment.author = getOrCreateContributor(authService.getUserForUserId(authService.getUserId())?.displayName, authService.getUserId())

        boolean success
        if (json.parentCommentId) {
            Comment parent = Comment.findByUuid(json.parentCommentId);
            checkState parent

            comment.uuid = UUID.randomUUID().toString()
            success = save comment

            if (success) {
                parent.addToChildren(comment)
                success &= save parent
            }
        } else {
            success = save comment
        }

        if (!success) {
            comment = null
        }

        comment
    }

    boolean updateComment(String commentId, Map json) {
        checkArgument commentId
        checkArgument json

        Comment comment = Comment.findByUuid(commentId)
        checkState comment

        comment.text = json.text

        save comment
    }

    boolean deleteComment(String commentId) {
        checkArgument commentId

        Comment comment = Comment.findByUuid(commentId)
        checkState comment

        delete comment
    }

    List<Comment> getCommentsForProfile(String profileId) {
        checkArgument profileId

        // we only want top level comments (i.e. where the parent is empty) - child comments will be pulled in via the one to many relationship
        Comment.findAllByProfileUuidAndParentIsNull(profileId)
    }
}
