package au.org.ala.profile

import org.springframework.web.multipart.commons.CommonsMultipartFile

class AttachmentService {

    def grailsApplication

    boolean deleteAttachment(String opusId, String profileId, String attachmentId, String extension) {
        File file = new File(getPath(opusId, profileId, attachmentId, extension))

        file.delete()
    }

    void saveAttachment(String opusId, String profileId, String attachmentId, CommonsMultipartFile incomingFile, String extension) {
        File file = new File(getPath(opusId, profileId, attachmentId, extension))
        file.mkdirs()
        incomingFile.transferTo(file)
    }

    File getAttachment(String opusId, String profileId, String attachmentId, String extension) {
        File file = new File(getPath(opusId, profileId, attachmentId, extension))

        file.exists() ? file : null
    }

    String getPath(String opusId, String profileId, String attachmentId, String extension) {
        "${grailsApplication.config.attachments.directory}/${opusId}/${profileId ? profileId + '/' : ''}${attachmentId}.${extension}"
    }
}
