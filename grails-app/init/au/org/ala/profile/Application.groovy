package au.org.ala.profile

import au.org.ala.names.search.ALANameSearcher
import au.org.ala.profile.listener.AuditListener
import au.org.ala.profile.listener.LastUpdateListener
import au.org.ala.profile.listener.ValueConverterListener
import au.org.ala.profile.sanitizer.SanitizedHtml
import au.org.ala.profile.sanitizer.SanitizerPolicy
import au.org.ala.web.AuthService
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    ALANameSearcher nameSearcher(@Value('${name.index.location}') String indexLocation) {
        indexLocation ?  new ALANameSearcher(indexLocation) : new ALANameSearcher()
    }

    @Bean
    AuditListener auditListener(@Autowired Datastore datastore, @Autowired AuditService auditService) {
        return new AuditListener(datastore, auditService)
    }

    @Bean
    LastUpdateListener lastUpdateListener(@Autowired Datastore datastore, @Autowired AuthService authService) {
        return new LastUpdateListener(datastore, authService)
    }

    @Bean(name = 'htmlSanitizerListener')
    ValueConverterListener<SanitizedHtml, String> htmlSanitizerListener(@Autowired Datastore datastore, @Autowired SanitizerPolicy sanitizerPolicy) {
        return ValueConverterListener.of(datastore, SanitizedHtml, String, sanitizerPolicy.&sanitizeField)
    }

}