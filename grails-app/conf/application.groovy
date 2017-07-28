def appName = 'profile-service'
def ENV_NAME = "${appName.toUpperCase()}_CONFIG"
default_config = "/data/${appName}/config/${appName}-config.properties"
if(!grails.config.locations || !(grails.config.locations instanceof List)) {
    grails.config.locations = []
}
if(System.getenv(ENV_NAME) && new File(System.getenv(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified in environment: " + System.getenv(ENV_NAME)
    grails.config.locations.add "file:" + System.getenv(ENV_NAME)
} else if(System.getProperty(ENV_NAME) && new File(System.getProperty(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified on command line: " + System.getProperty(ENV_NAME)
    grails.config.locations.add "file:" + System.getProperty(ENV_NAME)
} else if(new File(default_config).exists()) {
    println "[${appName}] Including default configuration file: " + default_config
    grails.config.locations.add "file:" + default_config
} else {
    println "[${appName}] No external configuration file defined."
}

grails.cache.config = {
    provider {
        name "ehcache-profile-service-"+(new Date().format("yyyyMMddHHmmss"))
    }
}

nsl.name.export.cacheTime = 86400 // seconds
lists.items.cacheSpec = 'maximumSize=100,expireAfterWrite=1m'

security {
    cas {
        uriExclusionFilterPattern='/images.*,/css.*,/js.*,/less.*'
        uriFilterPattern=''
        authenticateOnlyIfLoggedInFilterPattern=''
    }
}

app.http.header.userId = "X-ALA-userId"


elasticSearch {
    searchMethodName = 'esSearch'
    countHitsMethodName = 'esCountHits'
    // see http://noamt.github.io/elasticsearch-grails-plugin/guide/configuration.html for defaults
    client.hosts = [[host: "localhost", port: 9300]]
    datastoreImpl = "mongoDatastore"
    bulkIndexOnStartup = false
}

environments {
    development {
        grails {
            // use something like FakeSMTP locally to test without actually sending emails.
            mail {
                host = "localhost"
                port = 1025
                props = ["mail.debug": "true"]
            }
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles"
            }
        }
        elasticSearch {
            client.mode = "transport"
        }
        security.cas.appServerName='http://devt.ala.org.au:8081'
    }
    test {
        app.file.upload.path = "./target/archive"
        // It does not have to be a real url but a valid one
        // Used for integration testing
        app.uploads.url = "http://devt.ala.org.au:8082/profile-service/document/download?filename="
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles-test"
            }
        }
        elasticSearch {
            client.mode = 'local'
            index.store.type = 'default'
        }
        security.cas.appServerName='http://devt.ala.org.au:8082'
    }
    production {
        grails {
            mail {
                host = "localhost"
                port = 25
                props = ["mail.debug": "false"]
            }
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles"
            }
        }
        elasticSearch {
            client.mode = "transport"
        }
    }
}