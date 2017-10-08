package au.org.ala.profile

import org.springframework.transaction.annotation.Transactional

import javax.persistence.PersistenceException

@Transactional
class VocabService extends BaseDataAccessService {

    boolean updateVocab(String vocabId, Map data) {
        log.debug("Updating vocabulary ${vocabId} with data ${data}")
        Vocab vocab = Vocab.findByUuid(vocabId);

        vocab.strict = data.strict as boolean

        if (data.deleteExisting) {
            vocab.terms.clear()
        }

        Set<String> retainedTermIds = []

        data.terms.each { item ->
            if (item.termId) {
                Term term = Term.findByUuid(item.termId)

                if (term) {
                    retainedTermIds << item.termId
                    term.name = item.name
                    term.order = item.order
                    term.required = item.required == null ? false : item.required.toBoolean()
                    term.summary = item.summary == null ? false : item.summary.toBoolean()
                    term.containsName = item.containsName == null ? false : item.containsName.toBoolean()
                }
            } else {
                // GRAILS-8061 beforeValidate does not get called on child records during a cascade save of the parent
                // Therefore, we cannot rely on the beforeValidate method of Term, which usually creates the UUID.
                Term term = new Term(uuid: UUID.randomUUID().toString(),
                        name: item.name,
                        order: item.order ?: vocab.terms.size() + 1,
                        required: item.required.toBoolean(),
                        summary: item.summary.toBoolean(),
                        containsName: item.containsName.toBoolean())
                term.vocab = vocab
                vocab.terms << term
            }
        }

        vocab.terms.each { item ->
            if (!retainedTermIds.contains(item.uuid)) {
                Term term = Term.findByUuid(item.uuid);
                if (term) {
                    term.vocab = null
                    log.debug("Deleting term ${item.name}")
                    term.delete()
                }
            }
        }

        save vocab
    }

    Term getOrCreateTerm(String name, String vocabId) {
        Term term

        Vocab vocab = Vocab.findByUuid(vocabId);

        if (vocab) {
            term = Term.findByVocabAndNameIlike(vocab, name)

            if (!term) {
                if (vocab.strict) {
                    throw new IllegalStateException("Term ${name} does not exist in the vocabulary, and cannot be created because the vocabulary is Strict.")
                } else {
                    term = new Term(name: name, vocab: vocab)
                    boolean success = save term
                    if (!success) {
                        throw new PersistenceException("Failed to insert new term.")
                    }
                }
            }
        } else {
            throw new PersistenceException("Vocabulary with id ${vocabId} does not exist")
        }

        term
    }

    int findUsagesOfTerm(String opusId, String vocabId, String termName) {

        Term term = Term.findByVocabAndNameIlike(Vocab.findByUuid(vocabId), termName)

        Opus opus = Opus.findByUuid(opusId)
        if (opus.authorshipVocabUuid == vocabId) {
            List<Profile> profiles = Profile.findAll {authorship.category == term.id || draft.authorship.category == term.id}

            return profiles.size()

        } else {
            List<Attribute> attributes = Attribute.findAllByTitle(term)

            attributes.size()
        }
    }

    Map<String, Integer> replaceUsagesOfTerm(String opusId, jsonMap) {

        def json = jsonMap.list
        log.debug("Replacing vocabulary term usages: ${json}")

        Map<String, Integer> replacedUsages = [:]

        Opus opus = Opus.findByUuid(opusId)

        json.each { replacement ->
            int replaced = replaceTerm(opus, replacement.vocabId, replacement.existingTermName, replacement.newTermName)

            replacedUsages << [(replacement.existingTermName): replaced]
        }

        replacedUsages
    }

    def replaceTerm = { opus, vocabId, existingTermName, newTermName ->
        int replacedUsages = 0

        Term existingTerm = Term.findByVocabAndNameIlike(Vocab.findByUuid(vocabId), existingTermName)

        if (existingTerm) {
            Term newTerm = getOrCreateTerm(newTermName, vocabId)

            if (opus.authorshipVocabUuid == vocabId) {
                List<Profile> profiles = Profile.findAll {authorship.category == existingTerm.id || draft.authorship.category == existingTerm.id}

                profiles.each {
                    it.authorship.each {a ->
                        if (a.category == existingTerm) {
                            a.category = newTerm
                        }
                    }
                    it.draft.each {d ->
                        d.authorship.each { a ->
                            if (a.category == existingTerm) {
                                a.category = newTerm
                            }
                        }
                    }
                    save it

                    replacedUsages++
                }
            } else {

                List<Attribute> attributes = Attribute.findAllByTitle(existingTerm)

                attributes.each {
                    it.title = newTerm

                    save it

                    replacedUsages++
                }
            }

            existingTerm.delete()
        }

        replacedUsages
    }
}
