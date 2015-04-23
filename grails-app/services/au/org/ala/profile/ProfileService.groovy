package au.org.ala.profile

import au.org.ala.web.AuthService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

import java.text.SimpleDateFormat

@Transactional
class ProfileService extends BaseDataAccessService {

    VocabService vocabService
    NameService nameService
    AuthService authService

    Profile createProfile(String opusId, Map json) {
        checkArgument opusId
        checkArgument json

        Opus opus = Opus.findByUuid(opusId)

        checkState opus

        Profile profile = new Profile(json)
        profile.opus = opus

        List<String> guidList = nameService.getGuidForName(profile.scientificName)
        if (guidList && guidList.size() > 0) {
            profile.guid = guidList[0]
        }

        profile.authorship = [new Authorship(category: "Author", text: authService.getUserForUserId(authService.getUserId()).displayName)]

        boolean success = save profile

        if (!success) {
            profile = null
        }

        profile
    }

    boolean deleteProfile(String profileId) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)

        delete profile
    }

    Profile updateProfile(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("privateMode")) {
            profile.privateMode = json.privateMode
        }

        saveImages(profileId, json)

        saveSpecimens(profileId, json)

        saveBibliography(profileId, json)

        saveAuthorship(profileId, json)

        boolean success = save profile

        if (!success) {
            profile = null
        }

        profile
    }

    boolean saveSpecimens(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("specimenIds") && json.specimenIds != profile.specimenIds) {
            if (profile.specimenIds) {
                profile.specimenIds.clear()
            } else {
                profile.specimenIds = []
            }
            profile.specimenIds.addAll(json.specimenIds ?: [])
        }

        if (!deferSave) {
            save profile
        }
    }

    boolean saveAuthorship(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        if (json.containsKey('authorship')) {
            Profile profile = Profile.findByUuid(profileId)
            checkState profile

            if (profile.authorship) {
                profile.authorship.clear()
            } else {
                profile.authorship = []
            }

            json.authorship.each {
                profile.authorship << new Authorship(it)
            }

            if (!deferSave) {
                save profile
            }
        }
    }

    boolean saveImages(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.primaryImage && json.primaryImage != profile.primaryImage) {
            profile.primaryImage = json.primaryImage
        }

        if (json.containsKey("excludedImages") && json.excludedImages != profile.excludedImages) {
            if (profile.excludedImages) {
                profile.excludedImages.clear()
            } else {
                profile.excludedImages = []
            }
            profile.excludedImages.addAll(json.excludedImages ?: [])
        }

        if (!deferSave) {
            save profile
        }
    }

    boolean saveBibliography(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("bibliography") && json.bibliography != profile.bibliography) {
            if (!profile.bibliography) {
                profile.bibliography = []
            }

            Set retainedIds = []
            List<Bibliography> incomingBibliographies = json.bibliography.collect {
                Bibliography bibliography
                if (it.uuid) {
                    retainedIds << it.uuid
                    bibliography = Bibliography.findByUuid(it.uuid)
                    checkState bibliography, "No matching bibliography for uuid ${it.uuid}"
                    bibliography.order = it.order ?: 0
                } else {
                    bibliography = new Bibliography(text: it.text, uuid: UUID.randomUUID().toString(), order: it.order)
                }

                bibliography
            }

            profile.bibliography.each {
                if (!retainedIds.contains(it.uuid)) {
                    delete it, false
                }
            }

            profile.bibliography.clear()

            profile.bibliography.addAll(incomingBibliographies)
        }

        if (!deferSave) {
            save profile
        }
    }

    List<String> saveBHLLinks(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        log.debug("Saving BHL links...")

        Profile profile = Profile.findByUuid(profileId)

        checkState profile

        if (profile.bhlLinks == null) {
            profile.bhlLinks = [] as Set
        }

        List<String> linkIds = []
        List<Link> linksToSave = []

        log.debug("profile = ${profile != null}; links: ${json.links.size()}")
        if (json.links) {
            json.links.each {
                log.debug("Saving link ${it.title}...")
                Link link
                if (it.uuid) {
                    link = Link.findByUuid(it.uuid)
                    checkState link, "No matching link for uuid ${it.uuid}"
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
        } else if (profile.bhlLinks) {
            Link.deleteAll(profile.bhlLinks)
            profile.bhlLinks.clear()
            save profile
            linkIds = []
        }

        linkIds
    }

    List<String> saveLinks(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)

        checkState profile

        List<String> linkIds = []
        List<Link> linksToSave = []

        if (json.links) {
            json.links.each {
                Link link
                if (it.uuid) {
                    link = Link.findByUuid(it.uuid)
                    checkState link, "No matching link for uuid ${it.uuid}"
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
        } else if (profile.links) {
            Link.deleteAll(profile.links)
            profile.links.clear()
            save profile
        }

        linkIds
    }

    Publication savePublication(String profileId, Map data, MultipartFile file) {
        checkArgument profileId
        checkArgument data
        checkArgument file

        Profile profile = Profile.findByUuid(profileId)

        checkState profile

        Publication publication = new Publication(data)
        publication.publicationDate = new SimpleDateFormat("yyyy-MM-dd").parse(data.publicationDate)
        publication.uploadDate = new Date()
        publication.userId = authService.getUserId()
        publication.uuid = UUID.randomUUID().toString()
        profile.addToPublications(publication)
        publication.profile = profile

        publication.saveMongoFile(file)

        save profile

        publication
    }

    boolean deletePublication(String publicationId) {
        checkArgument publicationId

        Publication publication = Publication.findByUuid(publicationId);
        checkState publication

        Profile profile = publication.profile
        profile.publications.remove(publication)

        save profile
        delete publication
    }

    def getPublicationFile(String publicationId) {
        checkArgument publicationId

        Publication publication = Publication.findByUuid(publicationId)

        publication.getMongoFile()
    }

    Set<Publication> listPublications(String profileId) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)

        profile?.publications
    }

    Attribute createAttribute(String profileId, Map data) {
        checkArgument profileId
        checkArgument data

        log.debug("Creating new attribute for profile ${profileId} with data ${data}")
        Profile profile = Profile.findByUuid(profileId)

        checkState profile

        List<Contributor> creators = []
        data.creators.each {
            creators << getOrCreateContributor(it)
        }

        List<Contributor> editors = []
        data.editors.each {
            editors << getOrCreateContributor(it)
        }

        // only add the current user as the creator if the attribute was not copied from another profile
        if (!data.original) {
            creators << getOrCreateContributor(data.userDisplayName, data.userId)
        }

        Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)

        Attribute attribute = new Attribute(
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
        profile.addToAttributes(attribute)

        boolean success = save profile
        if (!success) {
            attribute = null
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

    boolean updateAttribute(String attributeId, String profileId, Map data) {
        checkArgument attributeId
        checkArgument profileId
        checkArgument data

        log.debug("Updating attribute ${attributeId}")

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        Attribute attribute = Attribute.findByUuid(attributeId)
        checkState attribute

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

        save attribute
    }

    boolean deleteAttribute(String attributeId, String profileId) {
        checkArgument attributeId
        checkArgument profileId

        Attribute attr = Attribute.findByUuid(attributeId)
        checkState attr
        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        log.debug("Deleting attribute ${attr?.title} from profile ${profile?.scientificName}...")

        println profile.attributes.contains(attr)
        profile.attributes.remove(attr)
        println profile.attributes.size()

        save profile

        delete attr
    }
}
