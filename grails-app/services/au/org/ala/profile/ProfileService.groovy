package au.org.ala.profile

import au.org.ala.web.AuthService
import grails.transaction.Transactional
import org.springframework.web.multipart.MultipartFile

import java.text.SimpleDateFormat

@Transactional
class ProfileService extends BaseDataAccessService {
    VocabService vocabService
    NameService nameService
    AuthService authService

    Profile createProfile(String opusId, Map json) {
        Opus opus = Opus.findByUuid(opusId)

        Profile profile = new Profile(json)
        profile.opus = opus

        List<String> guidList = nameService.getGuidForName(profile.scientificName)
        if (guidList && guidList.size() > 0) {
            profile.guid = guidList[0]
        }

        boolean success = save profile, false

        if (!success) {
            profile = null
        }

        profile
    }

    boolean deleteProfile(String profileId) {
        Profile profile = Profile.findByUuid(profileId);

        delete profile
    }

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

    Publication savePublication(String profileId, Map data, MultipartFile file) {
        // todo how to handle the file?
        Profile profile = Profile.findByUuid(profileId)

        Publication publication = null

        if (profile) {
            publication = new Publication(data)
            publication.publicationDate = new SimpleDateFormat("yyyy-MM-dd").parse(data.publicationDate)
            publication.uploadDate = new Date()
            publication.userId = authService.getUserId()
            publication.uuid = UUID.randomUUID().toString()
            profile.addToPublications(publication)
            publication.profile = profile

            save profile

            publication.saveMongoFile(file)
        }

        publication
    }

    boolean deletePublication(String publicationId) {
        Publication publication = Publication.findByUuid(publicationId);

        Profile profile = publication.profile
        profile.publications.remove(publication)

        save publication

        delete publication
    }

    def getPublicationFile(String publicationId) {
        Publication publication = Publication.findByUuid(publicationId)

        publication.getMongoFile()
    }

    Set<Publication> listPublications(String profileId) {
        Profile profile = Profile.findByUuid(profileId)

        profile?.publications
    }

    Attribute createAttribute(String profileId, Map data) {
        log.debug("Creating new attribute for profile ${profileId} with data ${data}")
        Profile profile = Profile.findByUuid(profileId)

        List<Contributor> creators = []
        data.creators.each {
            creators << getOrCreateContributor(it)
        }

        List<Contributor> editors = []
        data.editors.each {
            editors << getOrCreateContributor(it)
        }

        creators << getOrCreateContributor(data.userDisplayName, data.userId)

        Attribute attribute = null
        if (profile) {
            Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)

            attribute = new Attribute(
                    uuid: UUID.randomUUID().toString(),
                    title: titleTerm,
                    text: data.text
            )
            attribute.creators = creators
            attribute.editors = editors

            if (data.original) {
                Attribute original = Attribute.findByUuid(data.original.uuid)
                attribute.original = original
            }

            attribute.profile = profile
            profile.attributes.add(attribute)

            boolean success = save profile
            if (!success) {
                attribute = null
            }
        }

        attribute
    }

    Contributor getOrCreateContributor(String name, String userId = null) {
        Contributor contributor = userId ? Contributor.findByUserId(userId) : Contributor.findByName(name)
        if (!contributor) {
            contributor = new Contributor(userId: userId, name: name)
            contributor.save(flush: true)
        }
        contributor
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

            def contributor = getOrCreateContributor(data.userDisplayName, data.userId)

            if (!attribute.editors) {
                attribute.editors = []
            }

            if (!attribute.editors.contains(contributor) && data.significantEdit) {
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

            save profile

            deleted = delete attr
        } else {
            log.error("Failed to find matching attribute for id ${attributeId} and/or profile for id ${profileId}")
        }

        deleted
    }
}
