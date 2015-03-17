package au.org.ala.profile

import grails.transaction.Transactional

@Transactional
class ProfileService extends BaseDataAccessService {
    VocabService vocabService

    List<String> saveBHLLinks(String profileId, Map json) {
        log.debug("Saving BHL links...")

        Profile profile = Profile.findByUuid(profileId)
        List<String> linkIds = []
        List<Link> linksToSave = []

        log.debug("profile = ${profile != null}; links: ${json.links.size()}")
        if (profile) {
            if (json.links) {
                json.links.each {
                    log.debug("Saving link ${it.title}...")
                    Link link
                    if (it.uuid) {
                        link = Link.findByUuid(it.uuid)
                    } else {
                        link = new Link(uuid: UUID.randomUUID().toString())
                    }

                    link.url = it.url
                    link.title = it.title
                    link.description = it.description
                    link.fullTitle = it.fullTitle
                    link.edition = it.edition
                    link.publisherName = it.publisherName
                    link.doi = it.doi
                    linksToSave << link
                    linkIds << link.uuid
                }

                profile.bhlLinks = linksToSave

                save profile
            }
        }

        linkIds
    }

    List<String> saveLinks(String profileId, Map json) {
        Profile profile = Profile.findByUuid(profileId)

        List<String> linkIds = []
        List<Link> linksToSave = []

        if (profile) {
            if (json.links) {
                json.links.each {
                    Link link
                    if (it.uuid) {
                        link = Link.findByUuid(it.uuid)
                    } else {
                        link = new Link(uuid: UUID.randomUUID().toString())
                    }
                    link.url = it.url
                    link.title = it.title
                    link.description = it.description
                    link.errors.allErrors.each {
                        println it
                    }
                    linksToSave << link
                    linkIds << link.uuid
                }
                profile.links = linksToSave

                save profile
            }
        }

        linkIds
    }

    Attribute createAttribute(String profileId, Map data) {
        log.debug("Creating new attribute for profile ${profileId} with data ${data}")
        Profile profile = Profile.findByUuid(profileId)

        Contributor contributor = Contributor.findByUserId(data.userId)
        if (!contributor) {
            contributor = new Contributor(userId: data.userId, name: data.userDisplayName)
            contributor.save(flush: true)
        }

        Attribute attribute = null
        if (profile) {
            Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)

            attribute = new Attribute(
                    uuid: UUID.randomUUID().toString(),
                    title: titleTerm,
                    text: data.text
            )
            attribute.creators = [contributor]

            profile.attributes.add(attribute)

            boolean success = save profile
            if (!success) {
                attribute = null
            }
        }

        attribute
    }

    boolean updateAttribute(String attributeId, Map data) {
        log.debug("Updating attribute ${attributeId}")

        Profile profile = Profile.findByUuid(data.profileId)

        Attribute attribute = Attribute.findByUuid(attributeId)

        boolean updated = false
        if (attribute) {
            if (data.title) {
                Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)
                attribute.title = titleTerm
            }
            attribute.text = data.text

            def contributor = Contributor.findByUserId(data.userId)
            if (!contributor) {
                contributor = new Contributor(userId: data.userId, name: data.userDisplayName)
                contributor.save(flush: true)
            }

            if (!attribute.editors) {
                attribute.editors = []
            }

            if (!attribute.editors.contains(contributor)) {
                attribute.editors << contributor
            }

            updated = save attribute
        }

        updated
    }

    boolean deleteAttribute(String attributeId, String profileId) {
        Attribute attr = Attribute.findByUuid(attributeId)
        Profile profile = Profile.findByUuid(profileId)

        log.debug("Deleting attribute ${attr?.title} from profile ${profile?.scientificName}...")

        boolean deleted = false;
        if (attr && profile) {
            log.debug("Removing attribute from Profile...")
            profile.removeFromAttributes(attr)
            profile.save(flush: true)

            attr.delete(flush: true)

            if (attr.errors.allErrors.size() > 0) {
                log.error("Failed to delete attribute with id ${attributeId}")
                attr.errors.each { log.error(it) }
                deleted = false
            } else {
                log.info("Attribute ${attributeId} deleted")
                deleted = true
            }
        } else {
            log.error("Failed to find matching attribute for id ${attributeId} and/or profile for id ${profileId}")
        }

        deleted
    }
}
