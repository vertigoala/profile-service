package au.org.ala.profile

import au.org.ala.names.model.LinnaeanRankClassification
import au.org.ala.names.search.ALANameSearcher
import org.springframework.transaction.annotation.Transactional

import javax.annotation.PostConstruct

@Transactional
class NameService {

    def grailsApplication
    ALANameSearcher nameSearcher

    @PostConstruct
    def init() {
        nameSearcher = new ALANameSearcher("${grailsApplication.config.name.index.location}")
    }

    def getGuidForName(String name) {
        LinnaeanRankClassification rankClassification = new LinnaeanRankClassification()
        rankClassification.setScientificName(name)
        nameSearcher.searchForAcceptedLsidDefaultHandling(rankClassification, true)
    }
}
