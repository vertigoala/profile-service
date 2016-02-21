package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Attachment {

    String uuid
    String filename
    String title
    String description
    String contentType
    String rightsHolder
    String rights
    String licence
    String creator
    Date createdDate

    static mapping = {
        title nullable: true
        description nullable: true
        contentType nullable: true
        rights nullable: true
        rightsHolder nullable: true
        creator nullable: true
        licence nullable: true
        createdDate nullable: true
    }
}
