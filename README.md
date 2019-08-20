# ala-cors-plugin [![Build Status](https://travis-ci.org/AtlasOfLivingAustralia/ala-cors-plugin.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/ala-cors-plugin)

## Description
ALA CORS plugin for use with ala-auth-plugin. This will enable CORS for all CAS authenticated services from trusted 
origins only. Services not CAS authenticated will be enabled for all origins.

The configuration uses regex for matching the ```allowedOrigins``` property. 


## Usage

Required:
```
compile "org.grails.plugins:ala-auth:3.1"
compile "org.grails.plugins:ala-cors:1.0"
```

Changes made to ala-cors setup in the configuration files will be applied without requiring a restart when also using:
```
compile "org.grails.plugins:ala-admin-plugin:2.2"
``` 

### Setup CORS for your app

Disable the default grails CORS filter:
```yaml:
grails:
  cors:
    enable: false
```
    

In your configuration you can override these default properties:

```yaml
security:
  cors:
    enable: true
    authenticated:
      allowedOrigins: 
        - '.*\.ala\.org\.au(:[0-9]+)?'
    unauthenticated:
      allowedOrigins:
        - '*'
      allowCredentials: false
```

These additional defaults are used when not overridden.
 - ```allowedMethods=['GET', 'POST', 'HEAD']```
 - ```allowedHeaders=['*']```
 - ```allowCredentials=true```
 - ```maxAge=1800```
  
### Details

The intended purpose is to limit CORS to authorised origins when a CAS login occurs than
unauthorised origins. Other services that do not perform a CAS login will not be limited.

This is disabled with:
- ```security.cors.enable = false```

CORS configuration is defined for authenticated services and unauthenticated services with the following maps.
- ```security.cors.authenticated```
- ```security.cors.unauthenticated```

CORS parameters available for ```security.cors.authenticated``` and ```security.cors.unauthenticated```:

| Property           | Contents                                            |
| ------------------ | --------------------------------------------------- |
| allowedOrigins     |  Array of Strings. Regex matching. '*' matches all. |
| allowedMethods     |  Array of Strings. Exact matching. '*' matches all. |
| allowCredentials   |  Boolean.                                           |
| allowedHeaders     |  Array of Strings. Exact matching. '*' matches all. |                                  |
| exposedHeaders     |  Array of Strings. Exact matching. '*' matches all. |                                  |
| maxAge             |  Long.                                              |

Order of response rules:

| CAS Pattern Matched                    |  Authentication state  |  Origin   | CORS configuration |
| -------------------------------------- | ---------------------- | --------- | ------------------ |
| uriExcludeFilterPattern                |  ANY                   |  ANY      | unauthenticated    |
| uriFilterPattern                       |  ANY                   |  ALLOWED  | authenticated      |
| uriFilterPattern                       |  ANY                   |  ANY      | unauthenticated    |
| uriFilterAuthenticateIfLoggedInPattern |  LOGGED IN             |  ALLOWED  | authenticated      |
| uriFilterAuthenticateIfLoggedInPattern |  LOGGED IN             |  ANY      | unauthenticated    |
| uriFilterAuthenticateIfLoggedInPattern |  NOT LOGGED IN         |  ANY      | unauthenticated    |
