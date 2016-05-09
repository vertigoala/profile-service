package au.org.ala.profile

import au.org.ala.profile.util.ImageType
import grails.converters.JSON

class ImageController extends BaseController {

    def getImageInfo() {
        def imageId = params.imageId
        doWithProfileForImageId(imageId) { profile ->
                Map image = [
                        profileId: profile.uuid,
                         opusId   : profile.opus.uuid
                ]

                LocalImage privateImage = profile.privateImages?.find { it.imageId == imageId } ?: profile.draft?.privateImages?.find { it.imageId == imageId }
                LocalImage stagedImage = profile.draft?.stagedImages?.find { it.imageId == imageId }
                if (privateImage) {
                    image.putAll(privateImage.properties)
                    image.type = ImageType.PRIVATE.name()
                } else if (stagedImage) {
                    image.putAll(stagedImage.properties)
                    image.type = ImageType.STAGED.name()
                }
                image?.remove("dbo")

                render image as JSON
        }
    }

    def updateImageMetadata() {
        def imageId = params.imageId
        def metadata = request.getJSON()
        log.info("imageId: $imageId, metadata: $metadata")
        doWithProfileForImageId(imageId) { profile ->
            LocalImage image = profile.privateImages?.find { it.imageId == imageId } ?: profile.draft?.privateImages?.find { it.imageId == imageId }
            if (!image) {
                image = profile.draft?.stagedImages?.find { it.imageId == imageId }
            }
            if (image) {
                image.creator = metadata.creator
                image.description = metadata.description
                image.licence = metadata.licence
                image.rights = metadata.rights
                image.rightsHolder = metadata.rightsHolder
                image.title = metadata.title
                image.created = metadata.created

                profile.save()
                if (profile.hasErrors()) {
                    badRequest
                }
                render image as JSON
            } else {
                notFound "No image found with id $imageId"
            }
        }
    }

    private def doWithProfileForImageId(String imageId, Closure body) {
        if (!imageId) {
            badRequest "imageId is a required parameter"
        } else {
            // check for private images
            List<Profile> profiles = Profile.withCriteria {
                "privateImages" {
                    eq "imageId", imageId
                }
            }

            if (!profiles) {
                // check for draft private images
                profiles = Profile.withCriteria {
                    isNotNull "draft"
                    "draft" {
                        eq "privateImages.imageId", imageId
                    }
                }
            }

            if (!profiles) {
                // check for draft staged (i.e. public) images
                profiles = Profile.withCriteria {
                    isNotNull "draft"
                    "draft" {
                        eq "stagedImages.imageId", imageId
                    }
                }
            }

            if (!profiles) {
                notFound "No image was found with id $imageId"
            } else if (profiles?.size() == 1) {
                body(profiles[0])
            } else {
                badRequest "Non unique image id!" // should never happen
            }
        }
    }
}
