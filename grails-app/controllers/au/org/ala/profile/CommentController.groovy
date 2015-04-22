package au.org.ala.profile

import grails.converters.JSON

class CommentController extends BaseController {

    CommentService commentService

    def addComment() {
        def json = request.getJSON()

        if (!params.profileId || !json) {
            badRequest()
        } else {
            Profile profile = Profile.findByUuid(params.profileId)

            if (!profile) {
                notFound()
            } else {
                Comment comment = commentService.createComment(params.profileId, json)

                render comment as JSON
            }
        }
    }

    def updateComment() {
        def json = request.getJSON()

        if (!params.commentId || !json) {
            badRequest()
        } else {
            Comment comment = Comment.findByUuid(params.commentId)

            if (!comment) {
                notFound()
            } else {
                boolean updated = commentService.updateComment(params.commentId, json)

                success([updated: updated])
            }
        }
    }

    def getComments() {
        if (!params.profileId) {
            badRequest()
        } else {
            Profile profile = Profile.findByUuid(params.profileId)

            if (!profile) {
                notFound()
            } else {
                List<Comment> comments = commentService.getCommentsForProfile(params.profileId)

                render comments as JSON
            }
        }
    }

    def getComment() {
        if (!params.profileId || !params.commentId) {
            badRequest()
        } else {
            Profile profile = Profile.findByUuid(params.profileId)

            if (!profile) {
                notFound()
            } else {
                Comment comment = Comment.findByUuid(params.commentId)

                render comment as JSON
            }
        }
    }

    def deleteComment() {
        if (!params.commentId) {
            badRequest()
        } else {
            boolean deleted = commentService.deleteComment(params.commentId)


            success([deleted: deleted])
        }
    }
}
