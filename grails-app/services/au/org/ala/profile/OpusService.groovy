package au.org.ala.profile

import au.org.ala.profile.security.Role
import org.springframework.transaction.annotation.Transactional

@Transactional
class OpusService extends BaseDataAccessService {

    Opus createOpus(json) {
        log.debug("Creating new opus record")
        Opus opus = new Opus(json)

        Vocab attributeVocab = new Vocab(name: "${opus.title} Attribute Vocabulary", strict: false)
        save attributeVocab
        Vocab authorshipVocab = new Vocab(name: "${opus.title} Authorship Vocabulary", strict: false)
        Term authorTerm = new Term(name: "Author", vocab: authorshipVocab, uuid: UUID.randomUUID())
        authorshipVocab.addToTerms(authorTerm)
        save authorshipVocab

        opus.attributeVocabUuid = attributeVocab.uuid
        opus.authorshipVocabUuid = authorshipVocab.uuid

        Glossary glossary = new Glossary()
        save glossary

        opus.glossary = glossary

        boolean success = save opus
        if (success) {
            opus
        } else {
            null
        }
    }

    Opus updateOpus(opusId, json) {
        Opus opus = Opus.findByUuid(opusId)

        if (json.title && json.title != opus.title) {
            opus.title = json.title
        }
        if (json.containsKey("shortName") && json.shortName != opus.shortName) {
            opus.shortName = json.shortName ? json.shortName.toLowerCase() : null
        }
        if (json.containsKey("imageSources") && json.imageSources != opus.imageSources) {
            if (opus.imageSources) {
                opus.imageSources.clear()
            } else {
                opus.imageSources = []
            }
            opus.imageSources.addAll(json.imageSources)
        }
        if (json.containsKey("approvedLists") && json.approvedLists != opus.approvedLists) {
            if (opus.approvedLists) {
                opus.approvedLists.clear()
            } else {
                opus.approvedLists = []
            }
            opus.approvedLists.addAll(json.approvedLists)
        }
        if (json.containsKey("recordSources") && json.recordSources != opus.recordSources) {
            if (opus.recordSources) {
                opus.recordSources.clear()
            } else {
                opus.recordSources = []
            }
            opus.recordSources.addAll(json.recordSources)
        }
        if (json.copyrightText != opus.copyrightText) {
            opus.copyrightText = json.copyrightText
        }
        if (json.footerText != opus.footerText) {
            opus.footerText = json.footerText
        }
        if (json.contact) {
            if (json.contact.email != opus.email) {
                opus.email = json.contact.email
            }
            if (json.contact.facebook != opus.facebook) {
                opus.facebook = json.contact.facebook
            }
            if (json.contact.twitter != opus.twitter) {
                opus.twitter = json.contact.twitter
            }
        }
        if (json.logoUrl && json.logoUrl != opus.logoUrl) {
            opus.logoUrl = json.logoUrl
        }
        if (json.bannerUrl && json.bannerUrl != opus.bannerUrl) {
            opus.bannerUrl = json.bannerUrl
        }
        if (json.thumbnailUrl && json.thumbnailUrl != opus.thumbnailUrl) {
            opus.thumbnailUrl = json.thumbnailUrl
        }
        if (json.mapAttribution && json.mapAttribution != opus.mapAttribution) {
            opus.mapAttribution = json.mapAttribution
        }
        if (json.mapPointColour && json.mapPointColour != opus.mapPointColour) {
            opus.mapPointColour = json.mapPointColour;
        }
        if (json.mapDefaultLatitude && json.mapDefaultLatitude != opus.mapDefaultLatitude) {
            opus.mapDefaultLatitude = json.mapDefaultLatitude as Float
        }
        if (json.mapDefaultLongitude && json.mapDefaultLongitude != opus.mapDefaultLongitude) {
            opus.mapDefaultLongitude = json.mapDefaultLongitude as Float
        }
        if (json.mapZoom && json.mapZoom != opus.mapZoom) {
            opus.mapZoom = json.mapZoom as int
        }
        if (json.mapBaseLayer && json.mapBaseLayer != opus.mapBaseLayer) {
            opus.mapBaseLayer = json.mapBaseLayer
        }
        if (json.biocacheUrl && json.biocacheUrl != opus.biocacheUrl) {
            opus.biocacheUrl = json.biocacheUrl
        }
        if (json.biocacheName && json.biocacheName != opus.biocacheName) {
            opus.biocacheName = json.biocacheName
        }
        if (json.containsKey("keybaseProjectId") && json.keybaseProjectId != opus.keybaseProjectId) {
            opus.keybaseProjectId = json.keybaseProjectId
        }
        if (json.containsKey("keybaseKeyId") && json.keybaseKeyId != opus.keybaseKeyId) {
            opus.keybaseKeyId = json.keybaseKeyId
        }
        if (json.has("enablePhyloUpload") && json.enablePhyloUpload != opus.enablePhyloUpload) {
            opus.enablePhyloUpload = json.enablePhyloUpload as boolean
        }
        if (json.has("enableOccurrenceUpload") && json.enableOccurrenceUpload != opus.enableOccurrenceUpload) {
            opus.enableOccurrenceUpload = json.enableOccurrenceUpload as boolean
        }
        if (json.has("enableTaxaUpload") && json.enableTaxaUpload != opus.enableTaxaUpload) {
            opus.enableTaxaUpload = json.enableTaxaUpload as boolean
        }
        if (json.has("enableKeyUpload") && json.enableKeyUpload != opus.enableKeyUpload) {
            opus.enableKeyUpload = json.enableKeyUpload as boolean
        }
        if (json.has("showLinkedOpusAttributes") && json.showLinkedOpusAttributes != opus.showLinkedOpusAttributes) {
            opus.showLinkedOpusAttributes = json.showLinkedOpusAttributes as boolean
        }
        if (json.has("allowCopyFromLinkedOpus") && json.allowCopyFromLinkedOpus != opus.allowCopyFromLinkedOpus) {
            opus.allowCopyFromLinkedOpus = json.allowCopyFromLinkedOpus as boolean
        }
        if (json.has("allowFineGrainedAttribution") && json.allowFineGrainedAttribution != opus.allowFineGrainedAttribution) {
            opus.allowFineGrainedAttribution = json.allowFineGrainedAttribution as boolean
        }
        if (json.supportingOpuses != null) {
            opus.supportingOpuses.clear()
            json.supportingOpuses.each {
                Opus supportingOpus = Opus.findByUuid(it.uuid);
                if (supportingOpus) {
                    opus.supportingOpuses << supportingOpus
                }
            }
        }

        boolean success = save opus

        if (success) {
            opus
        } else {
            null
        }
    }

    Opus updateAboutHtml(String opusId, String html) {
        checkArgument opusId

        Opus opus = Opus.findByUuid(opusId)
        checkState opus

        if (html != opus.aboutHtml) {
            opus.aboutHtml = html
        }

        save opus

        opus
    }

    boolean updateUsers(String opusId, Map json) {
        checkArgument opusId
        checkArgument json

        Opus opus = Opus.findByUuid(opusId)
        checkState opus

        if (json.containsKey("authorities")) {
            if (opus.authorities) {
                Authority.deleteAll(opus.authorities)
                opus.authorities.clear()
            } else {
                opus.authorities = []
            }

            json.authorities?.each {
                Contributor user = getOrCreateContributor(it.name, it.userId)
                Role role = Role.valueOf(it.role.toUpperCase())
                opus.authorities << new Authority(user: user, role: role, notes: it.notes)
            }
        }

        save opus
    }

    boolean deleteOpus(String opusId) {
        Opus opus = Opus.findByUuid(opusId);

        List profiles = Profile.findAllByOpus(opus)
        Profile.deleteAll(profiles)

        if (opus.glossary) {
            GlossaryItem glossaryItems = GlossaryItem.findByGlossary(opus.glossary)
            if (glossaryItems) {
                GlossaryItem.deleteAll(glossaryItems)
            }
            delete opus.glossary
        }

        if (opus.attributeVocabUuid) {
            Vocab vocab = Vocab.findByUuid(opus.attributeVocabUuid)
            Term.deleteAll(vocab.terms)
            delete vocab
        }
        if (opus.authorshipVocabUuid) {
            Vocab vocab = Vocab.findByUuid(opus.authorshipVocabUuid)
            Term.deleteAll(vocab.terms)
            delete vocab
        }

        List<Opus> linkedOpuses = Opus.findAllBySupportingOpuses(opus)
        linkedOpuses?.each {
            it.supportingOpuses.remove(opus)
            save it
        }

        delete opus
    }

    boolean deleteGlossaryItem(String glossaryItemId) {
        GlossaryItem item = GlossaryItem.findByUuid(glossaryItemId);

        boolean success = false

        if (item) {
            item.glossary.items.remove(item);

            save item.glossary

            success = delete item
        }

        return success
    }

    boolean updateGlossaryItem(String glossaryItemId, data) {
        GlossaryItem item = GlossaryItem.findByUuid(glossaryItemId);

        boolean success = false

        if (item) {
            updateItemDetails(item, data)

            success = save item
        }

        return success
    }

    GlossaryItem createGlossaryItem(String opusId, data) {
        Opus opus = Opus.findByUuid(opusId)

        GlossaryItem item = new GlossaryItem();
        item.uuid = UUID.randomUUID().toString()
        updateItemDetails(item, data)

        item.glossary = opus.glossary

        opus.glossary.items << item

        boolean success = save opus.glossary

        if (!success) {
            item = null
        }

        item
    }

    private updateItemDetails(GlossaryItem item, data) {
        if (data.description && data.description != item.description) {
            item.description = data.description
        }
        if (data.term && data.term != item.term) {
            item.term = data.term
        }
        if (data.cf != null) {
            item.cf.clear()

            data.cf.each {
                GlossaryItem cfItem = GlossaryItem.findByTerm(it)
                if (cfItem) {
                    item.cf << cfItem
                } else {
                    log.warn("Could find matching cf item ${it}. Ignoring.")
                }
            }
        }
    }

    boolean saveGlossaryItems(String opusId, Map json) {
        Glossary glossary

        if (json.glossaryId) {
            glossary = Glossary.findByUuid(json.glossaryId)
        } else if (json.opusId) {
            Opus opus = Opus.findByUuid(opusId)

            glossary = opus.glossary

            if (!glossary) {
                glossary = new Glossary()
                save glossary

                opus.glossary = glossary
                save opus
            }
        } else {
            throw new IllegalArgumentException("Could not find glossary or opus")
        }


        if (glossary.items) {
            GlossaryItem.deleteAll(glossary.items)
            glossary.items?.clear()
        } else if (glossary.items == null) {
            glossary.items = []
        }

        json.items.each {
            GlossaryItem item
            if (it["glossaryItemId"]) {
                item = GlossaryItem.findByUuid(it.glossaryItemId)
            } else {
                item = new GlossaryItem()
                item.uuid = UUID.randomUUID().toString()
                item.glossary = glossary
            }

            if (item) {
                updateItemDetails(item, it)

                glossary.items << item
            }
        }

        save glossary
    }
}
