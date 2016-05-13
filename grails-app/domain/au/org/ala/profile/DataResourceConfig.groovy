package au.org.ala.profile

import au.org.ala.profile.util.DataResourceOption

/**
 * Embedded entity to encapsulate the configuration items that control the data resources (collectory resources) used by the collection
 */
class DataResourceConfig {

    DataResourceOption imageResourceOption
    // a list of dr ids (if recordResourceOption = RESOURCES) or dh ids (if recordResourceOption = HUBS) that are providing images
    List<String> imageSources

    DataResourceOption recordResourceOption
    // a list of dr ids (if recordResourceOption = RESOURCES) or dh ids (if recordResourceOption = HUBS) that are providing records
    List<String> recordSources

    static constraints = {
        imageResourceOption nullable: true
        recordResourceOption nullable: true
    }

    static mapping = {
        imageResourceOption defaultValue: DataResourceOption.NONE
        recordResourceOption defaultValue: DataResourceOption.NONE
    }
}
