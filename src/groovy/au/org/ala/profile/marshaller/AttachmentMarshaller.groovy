package au.org.ala.profile.marshaller

import au.org.ala.profile.Attachment
import au.org.ala.profile.util.Utils
import grails.converters.JSON

class AttachmentMarshaller {

    void register() {
        JSON.registerObjectMarshaller(Attachment) { Attachment attachment ->
            return [
                    uuid        : attachment.uuid,
                    title       : attachment.title,
                    filename    : attachment.filename,
                    description : attachment.description,
                    rights      : attachment.rights,
                    rightsHolder: attachment.rightsHolder,
                    contentType : attachment.contentType,
                    licence     : attachment.licence,
                    licenceIcon : Utils.getCCLicenceIcon(attachment.licence),
                    creator     : attachment.creator,
                    createdDate : attachment.createdDate
            ]
        }
    }

}
