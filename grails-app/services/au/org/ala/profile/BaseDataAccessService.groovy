package au.org.ala.profile


class BaseDataAccessService {

    boolean save(entity, flush = true) {
        entity.save(flush: flush)

        boolean saved

        if (entity.errors.allErrors.size() > 0) {
            log.error("Failed to save ${entity}")
            entity.errors.each { log.error(it) }
            saved = false
        } else {
            log.info("Saved ${entity}")
            saved = true
        }

        saved
    }
}
