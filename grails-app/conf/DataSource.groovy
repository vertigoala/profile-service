environments {
    development {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "taxon-profile"
            }
        }
    }
    test {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "taxon-profile-test"
            }
        }
    }
    production {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "taxon-profile"
            }
        }
    }
}