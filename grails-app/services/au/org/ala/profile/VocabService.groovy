package au.org.ala.profile

import grails.transaction.Transactional

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
                }
            } else {
                // GRAILS-8061 beforeValidate does not get called on child records during a cascade save of the parent
                // Therefore, we cannot rely on the beforeValidate method of Term, which usually creates the UUID.
                Term term = new Term(uuid: UUID.randomUUID().toString(), name: item.name)
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
            term = Term.findByNameAndVocab(name, vocab)

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

    int findUsagesOfTerm(String vocabId, String termName) {
        Term term = Term.findByVocabAndName(Vocab.findByUuid(vocabId), termName)

        List<Attribute> attributes = Attribute.findAllByTitle(term)

        attributes.size()
    }

    Map<String, Integer> replaceUsagesOfTerm(json) {
        log.debug("Replacing vocabulary term usages: ${json}")

        Map<String, Integer> replacedUsages = [:]

        json.each { replacement ->
            int replaced = replaceTerm(json.vocabId, json.existingTermName, json.newTermName)

            replacedUsages << [(json.existingTermName): replaced]
        }

        replacedUsages
    }

    def replaceTerm = { vocabId, existingTermName, newTermName ->
        int replacedUsages = 0

        Term existingTerm = Term.findByVocabAndName(Vocab.findByUuid(vocabId), existingTermName)

        if (existingTerm) {
            Term newTerm = getOrCreateTerm(newTermName, vocabId)

            List<Attribute> attributes = Attribute.findAllByTitle(existingTerm)

            attributes.each {
                it.title = newTerm

                save it

                replacedUsages++
            }

            existingTerm.delete()
        }

        replacedUsages
    }
}
