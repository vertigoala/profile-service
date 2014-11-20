environments {
    development {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles"
            }
        }
    }
    test {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles-test"
            }
        }
    }
    production {
        grails {
            mongo {
                host = "localhost"
                port = "27017"
                databaseName = "profiles"
            }
        }
    }
}