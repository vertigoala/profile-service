package au.org.ala.profile

import au.org.ala.profile.util.DraftUtil
import au.org.ala.profile.util.ImageOption
import au.org.ala.profile.util.StorageExtension
import au.org.ala.profile.util.Utils
import au.org.ala.web.AuthService
import org.apache.commons.lang3.StringUtils
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

import java.text.SimpleDateFormat

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Transactional
class ProfileService extends BaseDataAccessService {

    VocabService vocabService
    NameService nameService
    AuthService authService
    BieService bieService
    DoiService doiService
    AttachmentService attachmentService
    def grailsApplication

    Map checkName(String opusId, String name) {
        Map result = [providedName: name, providedNameDuplicates: [], matchedName: [:], matchedNameDuplicates: []]

        Opus opus = Opus.findByUuid(opusId)
        checkState opus

        // 1. search for the name as it was provided to check for duplicates, searching as both the scientific name and the full name
        List providedScientificNameDuplicates = findByName(name, opus)
        if (providedScientificNameDuplicates) {
            result.providedNameDuplicates = providedScientificNameDuplicates.collect {
                [profileId: it.uuid, scientificName: it.scientificName, fullName: it.fullName, nameAuthor: it.nameAuthor]
            }
        }

        // 2. attempt to match the name
        Map matchedName = nameService.matchName(name)
        if (matchedName) {
            result.matchedName = [scientificName: matchedName.scientificName, fullName: matchedName.fullName, nameAuthor: matchedName.author, guid: matchedName.guid]

            List matchedScientificNameDuplicates = findByName(result.matchedName.scientificName, opus)
            if (matchedScientificNameDuplicates) {
                result.matchedNameDuplicates = matchedScientificNameDuplicates.collect {
                    [profileId: it.uuid, scientificName: it.scientificName, fullName: it.fullName, nameAuthor: it.nameAuthor]
                }
            }
            List matchedFullNameDuplicates = findByName(result.matchedName.fullName, opus)
            if (matchedFullNameDuplicates) {
                result.matchedNameDuplicates = matchedFullNameDuplicates.collect {
                    [profileId: it.uuid, scientificName: it.scientificName, fullName: it.fullName, nameAuthor: it.nameAuthor]
                }
            }
        }

        result
    }

    private List<Profile> findByName(String name, Opus opus) {
        Profile.withCriteria {
            eq "opus", opus

            or {
                ilike "scientificName", name
                ilike "fullName", name
                ilike "draft.scientificName", name
                ilike "draft.fullName", name
            }

            isNull "archivedDate" // ignore archived profiles during name checks
        }
    }

    Profile createProfile(String opusId, Map json) {
        checkArgument opusId
        checkArgument json

        Opus opus = Opus.findByUuid(opusId)

        checkState opus

        Profile profile = new Profile(json)
        profile.opus = opus

        Map matchedName = nameService.matchName(json.scientificName, [:], json.manuallyMatchedGuid ?: null)

        updateNameDetails(profile, matchedName, json.scientificName)

        if (authService.getUserId()) {
            Term term = vocabService.getOrCreateTerm("Author", opus.authorshipVocabUuid)
            profile.authorship = [new Authorship(category: term, text: authService.getUserForUserId(authService.getUserId()).displayName)]
        }

        boolean success = save profile

        if (!success) {
            profile = null
        }

        profile
    }

    private void updateNameDetails(profile, Map matchedName, String providedName) {
        if (matchedName) {
            if (providedName.equalsIgnoreCase(matchedName.fullName) || providedName.equalsIgnoreCase(matchedName.scientificName)) {
                profile.scientificName = matchedName.scientificName
                profile.fullName = matchedName.fullName
                profile.nameAuthor = matchedName.nameAuthor
            } else {
                profile.scientificName = providedName
                profile.fullName = providedName
                profile.nameAuthor = null
            }
            profile.matchedName = new Name(matchedName)
            profile.guid = matchedName.guid
            populateTaxonHierarchy(profile)
        } else {
            profile.scientificName = providedName
            profile.fullName = providedName
            profile.matchedName = null
            profile.nameAuthor = null
            profile.guid = null
            profile.classification = []
            profile.rank = null
        }

        // try to match the name against the NSL. If we get a match, and there is currently no name author, use the author from the NSL match
        Map matchedNSLName = nameService.matchNSLName(providedName)
        if (matchedNSLName) {
            profile.nslNameIdentifier = matchedNSLName.nslIdentifier
            profile.nslProtologue = matchedNSLName.nslProtologue
            if (!profile.nameAuthor) {
                profile.nameAuthor = matchedNSLName.nameAuthor
            }
        } else {
            profile.nslNameIdentifier = null
            profile.nslNomenclatureIdentifier = null
            profile.nslProtologue = null
        }
    }

    void renameProfile(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        def profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.newName) {
            Map matchedName = nameService.matchName(json.newName, [:], json.manuallyMatchedGuid ?: null)

            updateNameDetails(profileOrDraft(profile), matchedName, json.newName)
        }

        if (json.clearMatch?.booleanValue()) {
            updateNameDetails(profileOrDraft(profile), null, profileOrDraft(profile).scientificName)
        }

        boolean success = save profile

        if (!success) {
            profile = null
        }

        profile
    }

    void populateTaxonHierarchy(profile) {
        if (profile && profile.guid) {
            def classificationJson = bieService.getClassification(profile.guid)

            if (classificationJson) {
                profile.classification = classificationJson.collect {
                    // the classifications returned from the BIE service are in descending order, so the last one will be
                    // the rank for the profile
                    profile.rank = it.rank

                    new Classification(rank: it.rank, guid: it.guid, name: it.scientificName)
                }

                Map speciesProfile = bieService.getSpeciesProfile(profile.guid)
                profile.taxonomyTree = speciesProfile?.taxonConcept?.infoSourceName
            } else {
                log.info("Unable to find species classification for ${profile.scientificName}, with GUID ${profile.guid}")
            }
        }
    }

    Profile archiveProfile(String profileId, String archiveComment) {
        checkArgument profileId
        checkArgument archiveComment

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        profile.archiveComment = archiveComment
        profile.archivedBy = authService.getUserForUserId(authService.getUserId()).displayName
        profile.archivedDate = new Date()
        profile.archivedWithName = profile.scientificName
        profile.scientificName = "${profile.scientificName} (Archived ${new SimpleDateFormat('dd/MM/yyyy H:mm a').format(profile.archivedDate)})"

        save profile

        profile
    }

    Profile restoreArchivedProfile(String profileId, String newName = null) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (newName && Profile.findByOpusAndScientificName(profile.opus, newName)) {
            throw new IllegalStateException("A profile already exists with the name ${newName}")
        } else if (!newName && Profile.findByOpusAndScientificName(profile.opus, profile.archivedWithName)) {
            throw new IllegalStateException("A profile already exists with the name ${profile.archivedWithName}. Provide a new unique name.")
        }

        profile.archiveComment = null
        profile.archivedBy = null
        profile.archivedDate = null
        profile.scientificName = newName ?: profile.archivedWithName
        profile.archivedWithName = null

        save profile

        profile
    }

    boolean deleteProfile(String profileId) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (profile.publications) {
            throw new IllegalStateException("Profiles with published versions cannot be deleted. Use the archive option instead.")
        }

        delete profile
    }

    boolean discardDraftChanges(String profileId) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (profile.draft) {
            // clean up any draft attachment files
            List existingIds = profile.attachments?.collect { it.uuid } ?: []

            profile.draft.attachments?.each { draftAttachment ->
                if (!existingIds.contains(draftAttachment.uuid)) {
                    attachmentService.deleteAttachment(profile.opus.uuid, profile.uuid, draftAttachment.uuid, Utils.getFileExtension(draftAttachment.filename))
                }
            }

            profile.draft = null

            save profile
        }
    }

    boolean toggleDraftMode(String profileId) {
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (profile.draft) {
            // delete files for attachments that were removed during the draft stage
            profile.attachments?.each { attachment ->
                if (profile.draft.attachments?.find { it.uuid == attachment.uuid } == null) {
                    attachmentService.deleteAttachment(profile.opus.uuid, profile.uuid, attachment.uuid, Utils.getFileExtension(attachment.filename))
                }
            }

            DraftUtil.updateProfileFromDraft(profile)

            Set<Attribute> attributesToDelete = profile.attributes.findAll {
                String uuid = it.uuid
                profile.draft.attributes.find { it.uuid == uuid } == null
            }

            profile.draft = null

            attributesToDelete.each {
                profile.attributes.remove(it)
                delete it
            }


            save profile
        } else {
            profile.draft = DraftUtil.createDraft(profile)
            profile.draft.createdBy = authService.getUserForUserId(authService.getUserId()).displayName

            save profile
        }
    }

    def profileOrDraft(Profile profile) {
        profile.draft ?: profile
    }

    Profile updateProfile(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("nslNomenclatureIdentifier")
                && json.nslNomenclatureIdentifier != profileOrDraft(profile).nslNomenclatureIdentifier) {
            profileOrDraft(profile).nslNomenclatureIdentifier = json.nslNomenclatureIdentifier
        }

        saveImages(profile, json, true)

        saveSpecimens(profile, json, true)

        saveBibliography(profile, json, true)

        saveAuthorship(profile, json, true)

        boolean success = save profile

        if (!success) {
            profile = null
        }

        profile
    }

    boolean saveSpecimens(Profile profile, Map json, boolean deferSave = false) {
        checkArgument profile
        checkArgument json

        if (json.containsKey("specimenIds") && json.specimenIds != profileOrDraft(profile).specimenIds) {
            if (profileOrDraft(profile).specimenIds) {
                profileOrDraft(profile).specimenIds.clear()
            } else {
                profileOrDraft(profile).specimenIds = []
            }
            profileOrDraft(profile).specimenIds.addAll(json.specimenIds ?: [])

            if (!deferSave) {
                save profile
            }
        }
    }

    boolean saveAuthorship(Profile profile, Map json, boolean deferSave = false) {
        if (json.containsKey('authorship')) {
            if (profileOrDraft(profile).authorship) {
                profileOrDraft(profile).authorship.clear()
            } else {
                profileOrDraft(profile).authorship = []
            }

            json.authorship.each {
                Term term = vocabService.getOrCreateTerm(it.category, profile.opus.authorshipVocabUuid)
                profileOrDraft(profile).authorship << new Authorship(category: term, text: it.text)
            }

            if (!deferSave) {
                save profile
            }
        }
    }

    boolean saveAuthorship(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        saveAuthorship(profile, json)
    }

    boolean saveImages(Profile profile, Map json, boolean deferSave = false) {
        checkArgument profile
        checkArgument json

        def profileOrDraft = profileOrDraft(profile)

        if (json.containsKey("primaryImage") && json.primaryImage != profileOrDraft.primaryImage) {
            profileOrDraft.primaryImage = json.primaryImage
        }

        if (json.containsKey("imageSettings") && json.imageSettings != profileOrDraft.imageSettings) {
            if (profileOrDraft.imageSettings) {
                profileOrDraft.imageSettings.clear()
            } else {
                profileOrDraft.imageSettings = [:]
            }

            json.imageSettings?.each {
                ImageOption imageDisplayOption = ImageOption.byName(it.displayOption, profile.opus.approvedImageOption)
                if (imageDisplayOption == profile.opus.approvedImageOption) {
                    imageDisplayOption = null
                }
                profileOrDraft.imageSettings << [(it.imageId): new ImageSettings(imageDisplayOption: imageDisplayOption, caption: it.caption ?: '')]
            }
        }

        if (!deferSave) {
            save profile
        }
    }

    boolean recordStagedImage(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile
        checkState profile.draft
        // can only stage images for a draft profile - otherwise images are to be automatically updated

        boolean success = recordImage(profile.draft.stagedImages, json)

        if (success) {
            if (json.action == "delete") {
                profile.imageDisplayOptions?.remove(json.imageId)
            }
            success = save profile
        } else {
            log.error "Failed to record image (prior to saving the profile)"
        }

        success
    }

    boolean recordPrivateImage(String profileId, Map json) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        boolean success = recordImage(profileOrDraft(profile).privateImages, json)

        if (success) {
            if (json.action == "delete") {
                profile.imageDisplayOptions?.remove(json.imageId)
            }
            success = save profile
        } else {
            log.error "Failed to record image (prior to saving the profile)"
        }

        success
    }

    private static boolean recordImage(List<LocalImage> imageStore, Map json) {
        boolean success = false
        if (json.action == "add") {
            LocalImage image = new LocalImage()
            image.creator = json.multimedia[0].creator
            image.dateCreated = json.multimedia[0].dateCreated ? new SimpleDateFormat("yyyy-MM-dd").parse(json.multimedia[0].dateCreated) : null
            image.description = json.multimedia[0].description
            image.imageId = json.imageId
            image.licence = json.multimedia[0].licence
            image.originalFileName = json.multimedia[0].originalFilename
            image.rights = json.multimedia[0].rights
            image.rightsHolder = json.multimedia[0].rightsHolder
            image.title = json.multimedia[0].title
            image.contentType = json.multimedia[0].contentType

            imageStore << image
            success = true
        } else if (json.action == "delete") {
            LocalImage image = imageStore.find { it.imageId == json.imageId }
            success = imageStore.remove(image)
        }

        success
    }

    boolean saveBibliography(Profile profile, Map json, boolean deferSave = false) {
        checkArgument profile
        checkArgument json

        if (json.containsKey("bibliography") && json.bibliography != profileOrDraft(profile).bibliography) {
            profileOrDraft(profile).bibliography = json.bibliography.collect {
                new Bibliography(text: it.text, uuid: UUID.randomUUID().toString(), order: it.order)
            }

            if (!deferSave) {
                save profile
            }
        }
    }

    boolean saveBHLLinks(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("links") && json.links != profileOrDraft(profile).bhlLinks) {
            profileOrDraft(profile).bhlLinks = json.links.collect {
                Link link = new Link(uuid: UUID.randomUUID().toString())
                link.url = it.url
                link.title = it.title
                link.description = it.description
                link.fullTitle = it.fullTitle
                link.edition = it.edition
                link.publisherName = it.publisherName
                link.doi = it.doi
                link
            }

            if (!deferSave) {
                save profile
            }
        }
    }

    boolean saveLinks(String profileId, Map json, boolean deferSave = false) {
        checkArgument profileId
        checkArgument json

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        if (json.containsKey("links") && json.links != profileOrDraft(profile).links) {
            profileOrDraft(profile).links = json.links.collect {
                Link link = new Link(uuid: UUID.randomUUID().toString())
                link.url = it.url
                link.title = it.title
                link.description = it.description
                link
            }

            if (!deferSave) {
                save profile
            }
        }
    }

    /**
     *  Related Business Capability:  Allow users to keep historical versions of their profiles
     *  Related Application Feature: Save Snapshot Version feature of Profile Hub
     *
     *  A publication is either a PDF of the profile as a versioned snapshot in time OR
     *  if the profile has additional attachments, a ZIP file containing the profile PDF and
     *  any other file attachments.
     *
     * @param profileId
     * @param file - a pdf of the Profile that has already been generated and sent to this service
     * @return
     */
    def savePublication(String profileId, MultipartFile file) {
        checkArgument profileId
        checkArgument file

        Profile profile = Profile.findByUuid(profileId)

        checkState profile

        if (!profile.publications) {
            profile.publications = []
        }

        Publication publication = new Publication()
        publication.title = profile.scientificName
        publication.authors = profile.authorship.find { it.category.name == "Author" }?.text
        publication.publicationDate = new Date()
        publication.userId = authService.getUserId()
        publication.uuid = UUID.randomUUID().toString()
        if (profile.publications) {
            publication.version = profile.publications.sort { it.version }.last().version + 1
        } else {
            publication.version = 1
        }

        Map doiResult = doiService.mintDOI(profile.opus, publication)
        if (doiResult.status == "success") {
            publication.doi = doiResult.doi

            if (profile.attachments) {
                String fileName = "${grailsApplication.config.snapshot.directory}/${publication.uuid}.zip"
                savePublicationWithAttachments(profile, file, fileName)
                publication.setFileType(StorageExtension.ZIP)
            } else {
                String fileName = "${grailsApplication.config.snapshot.directory}/${publication.uuid}.pdf"
                //this copies the incoming 'file' data into a file object and saves this object to the file system
                file.transferTo(new File(fileName))
                publication.setFileType(StorageExtension.PDF)
            }
            profile.publications << publication
            save profile
            publication
        } else {
            doiResult
        }
    }

    File getPublicationFile(String publicationId) {
        checkArgument publicationId
        String extension = determineFileExtension(publicationId)
        String fileName = "${grailsApplication.config.snapshot.directory}/${publicationId}${extension}"

        new File(fileName)
    }

    /**
     * Makes and saves a publication for a profile that has attachments when user clicks
     * create snapshot version on Profile-Hub.
     * Takes the profile data, as sent via Profile-Hub as a multipartFile and all attachments for
     * this profile, bundles them up into a zip file and saves them to disk
     * @param profile
     * @param multipartFile - what will become a pdf of the profile
     * @param absoluteFileName
     */
    void savePublicationWithAttachments(Profile profile, MultipartFile multipartFile, String absoluteFileName) {
        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(absoluteFileName))
        Map<String,File> fileMap = [:]
        String profileName = fixFileName(StringUtils.removeEnd(profile.getScientificName(), '.'))
        fileMap.putAll(attachmentService.collectAllAttachmentsIncludingOriginalNames(profile))
        makeAndSaveZipFile(multipartFile, profileName, fileMap, outputStream)
    }

    String fixFileName(String profileName) {
        Calendar cal = Calendar.getInstance()
        profileName + ' - ' + cal.getTime() + StorageExtension.PDF.extension
    }

    private static void makeAndSaveZipFile(MultipartFile multipartFile, String profileName, Map<String,File> fileMap, ZipOutputStream outputStream) {
        try {
            fileMap.each { file ->
                outputStream.putNextEntry(new ZipEntry(file.key))
                file.value.withInputStream
                        { inputStream ->
                            outputStream << inputStream
                        }
                outputStream.closeEntry()
            }
            outputStream.putNextEntry(new ZipEntry(profileName))
            outputStream << multipartFile.getInputStream()
            outputStream.closeEntry()
        } finally {
            outputStream?.flush()
            outputStream?.close()
        }
    }

    //Publications can be either a pdf or zip, only the publication knows what it is, publications
    //are only accessible via their parent profile
    private String determineFileExtension(String publicationid) {
        Profile profile = getProfileFromPubId(publicationid)
        List<Publication> publicationList = profile?.getPublications()
        Publication publication = publicationList?.find { item -> item.uuid.equals(publicationid) }
        String fileExtension = publication.getFileType().extension
        return fileExtension
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
            def contributor
            if (data.attributeTo) {
                contributor = getOrCreateContributor(data.attributeTo)
            } else {
                contributor = getOrCreateContributor(data.userDisplayName, data.userId)
            }
            creators << contributor
        }

        // if we're copying the attribute from another collection and the significant edit flag is set, then add the user as an editor
        if (data.original && data.significantEdit) {
            def contributor
            if (data.attributeTo) {
                contributor = getOrCreateContributor(data.attributeTo)
            } else {
                contributor = getOrCreateContributor(data.userDisplayName, data.userId)
            }
            editors << contributor
        }

        Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)

        Attribute attribute = new Attribute(
                uuid: UUID.randomUUID().toString(),
                title: titleTerm,
                text: data.text,
                source: data.source
        )
        attribute.creators = creators
        attribute.editors = editors

        if (data.original) {
            Attribute original = Attribute.findByUuid(data.original.uuid)
            attribute.original = original
        }

        if (profile.draft) {
            if (!profile.draft.attributes) {
                profile.draft.attributes = []
            }
            profile.draft.attributes << attribute
        } else {
            attribute.profile = profile
            profile.addToAttributes(attribute)
        }

        profileOrDraft(profile).lastAttributeChange = "Added ${attribute.title.name}"

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

        Attribute attribute
        if (profile.draft) {
            attribute = profile.draft.attributes.find { it.uuid == attributeId }
        } else {
            attribute = profile.attributes.find { it.uuid == attributeId }
        }
        checkState attribute

        if (data.title) {
            Term titleTerm = vocabService.getOrCreateTerm(data.title, profile.opus.attributeVocabUuid)
            attribute.title = titleTerm
        }
        attribute.text = data.text
        attribute.source = data.source

        def contributor
        if (data.attributeTo) {
            contributor = getOrCreateContributor(data.attributeTo)
        } else {
            contributor = getOrCreateContributor(data.userDisplayName, data.userId)
        }

        if (!attribute.editors) {
            attribute.editors = []
        }

        if (!attribute.editors.contains(contributor) && data.significantEdit) {
            attribute.editors << contributor
        }

        profileOrDraft(profile).lastAttributeChange = "Updated ${attribute.title.name} - ${Utils.cleanupText(attribute.text)}"

        save profile
    }

    boolean deleteAttribute(String attributeId, String profileId) {
        checkArgument attributeId
        checkArgument profileId

        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        Attribute attr
        if (profile.draft) {
            attr = profile.draft.attributes.find { it.uuid == attributeId }
            profile.draft.attributes.remove(attr)
            profile.draft.lastAttributeChange = "Deleted ${attr.title.name} - ${Utils.cleanupText(attr.text)}"

            save profile
        } else {
            attr = profile.attributes.find { it.uuid == attributeId }
            checkState attr

            profile.attributes.remove(attr)

            profile.lastAttributeChange = "Deleted ${attr.title.name} - ${Utils.cleanupText(attr.text)}"

            save profile

            delete attr
        }
    }

    Profile getProfileFromPubId(String pubId) {
        List<Profile> profiles = Profile.withCriteria {
            eq("publications.uuid", pubId)
        };
        profiles.size() > 0 ? profiles.get(0) : null;
    }

    List<Attachment> saveAttachment(String profileId, Map metadata, CommonsMultipartFile file) {
        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        Date createdDate = metadata.createdDate ? new SimpleDateFormat("yyyy-MM-dd").parse(metadata.createdDate) : null

        if (metadata.uuid) {
            Attachment existing = profileOrDraft(profile).attachments.find { it.uuid == metadata.uuid }
            if (existing) {
                existing.title = metadata.title
                existing.description = metadata.description
                existing.rights = metadata.rights
                existing.rightsHolder = metadata.rightsHolder
                existing.licence = metadata.licence
                existing.creator = metadata.creator
                existing.createdDate = createdDate
            }
        } else {
            String extension = Utils.getFileExtension(file.originalFilename)
            Attachment newAttachment = new Attachment(uuid: UUID.randomUUID().toString(),
                    title: metadata.title, description: metadata.description, filename: metadata.filename,
                    contentType: file.contentType, rights: metadata.rights, createdDate: createdDate,
                    rightsHolder: metadata.rightsHolder, licence: metadata.licence, creator: metadata.creator)
            attachmentService.saveAttachment(profile.opus.uuid, profile.uuid, newAttachment.uuid, file, extension)
            profileOrDraft(profile).attachments << newAttachment
        }

        save profile

        profile.attachments
    }

    List<Attachment> deleteAttachment(String profileId, String attachmentId) {
        Profile profile = Profile.findByUuid(profileId)
        checkState profile

        Attachment attachment = profileOrDraft(profile).attachments?.find { it.uuid == attachmentId }
        if (attachment) {
            profileOrDraft(profile).attachments.remove(attachment)

            // Only delete the file if we are not in draft mode.
            // If we are in draft mode, then the file will be deleted when the draft is published.
            if (!profile.draft) {
                attachmentService.deleteAttachment(profile.opus.uuid, profile.uuid, attachment.uuid, Utils.getFileExtension(attachment.filename))
            }
        }

        save profile

        profile.attachments
    }
}
