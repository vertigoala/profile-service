package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class StagedImage {

    String imageId
    String originalFileName
    String title
    String description
    String rightsHolder
    String rights
    String licence
    String creator
    Date dateCreated
}
