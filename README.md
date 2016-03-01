# profile-service
Master: [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/profile-service.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/profile-service)

Dev: [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/profile-service.svg?branch=dev)](https://travis-ci.org/AtlasOfLivingAustralia/profile-service)

Profile Service

Related documentation for the Profile Service is also included in the [README.md](https://github.com/AtlasOfLivingAustralia/profile-hub/blob/master/README.md) file for the related Profile Hub application.

## Testing

Profile service includes a suite of integration tests which require a running MongoDB instance. I have not been able to get an embedded instance of MongoDB to work, so you will need to have a running, external instance to run the integration tests.

The Travis file includes a directive to install MongoDB, so the integration tests work on the CI server.

## Gotchas:

1. Cascade delete does not appear to be working with the mongodb plugin (version 3.0.3), so all delete methods have to manually delete the record rather than just removing it from, and then saving, the parent.
1. GRAILS-8061 beforeValidate does not get called on child records during a cascade save of the parent. Therefore, we cannot rely on the beforeValidate method in our entities to generate the UUID when saving via cascade.