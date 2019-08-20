package au.org.ala.web

import spock.lang.Specification

class CasCorsConfigurationSpec extends Specification {

    final static def testConfig1 = [
            allowedOrigins       : ['authorised\\.ala\\.org\\.au'],
            allowedHeaders       : ['*'],
            allowedMethods       : ["GET"],
            allowCredentials     : true,
            maxAge               : 1000,
            exposedHeaders       : ['Context-Type'],
            allowedOriginPatterns: ['^authorised\\.ala\\.org\\.au$']
    ]

    final static def testConfig2 = [
            allowedOrigins       : ['.*\\.org\\.au(:[0-9]+)?', 'authorised\\.ala\\.org\\.au'],
            allowedHeaders       : ['Context-Type'],
            allowedMethods       : ["POST", "GET"],
            allowCredentials     : true,
            maxAge               : 2000,
            exposedHeaders       : ['Context-Type'],
            allowedOriginPatterns: ['^.*\\.org\\.au(:[0-9]+)?$', '^authorised\\.ala\\.org\\.au$']
    ]

    final static def defaultConfig = [
            allowedOrigins       : ['*'],
            allowedHeaders       : ['*'],
            allowedMethods       : ['*'],
            allowCredentials     : true,
            maxAge               : 1800,
            exposedHeaders       : null,
            allowedOriginPatterns: ['^.*$']
    ]

    def "Test allowedOrigins change"() {
        when:

        def casCorsConfiguration = new CasCorsConfiguration(config)
        casCorsConfiguration.setAllowedOrigins(newAllowedOrigins)

        then:

        TestUtil.compareLists(casCorsConfiguration.allowedOriginPatterns, allowedOriginPatterns)

        where:

        config        || newAllowedOrigins            || allowedOriginPatterns
        null          || defaultConfig.allowedOrigins || defaultConfig.allowedOriginPatterns
        null          || testConfig1.allowedOrigins   || testConfig1.allowedOriginPatterns
        null          || testConfig2.allowedOrigins   || testConfig2.allowedOriginPatterns
        defaultConfig || testConfig1.allowedOrigins   || testConfig1.allowedOriginPatterns
        defaultConfig || testConfig2.allowedOrigins   || testConfig2.allowedOriginPatterns
        testConfig1   || defaultConfig.allowedOrigins || defaultConfig.allowedOriginPatterns
        testConfig1   || testConfig2.allowedOrigins   || testConfig2.allowedOriginPatterns
        testConfig2   || defaultConfig.allowedOrigins || defaultConfig.allowedOriginPatterns
        testConfig2   || testConfig1.allowedOrigins   || testConfig1.allowedOriginPatterns

    }

    def "Test CasCorsConfiguration initialisation"() {
        when:

        // CAS config is required
        def casCorsConfiguration = new CasCorsConfiguration(config)

        then:

        TestUtil.compareLists(casCorsConfiguration.allowedOrigins, expectedConfig.allowedOrigins)
        TestUtil.compareLists(casCorsConfiguration.allowedMethods, expectedConfig.allowedMethods)
        TestUtil.compareLists(casCorsConfiguration.allowedHeaders, expectedConfig.allowedHeaders)
        casCorsConfiguration.allowCredentials == expectedConfig.allowCredentials
        casCorsConfiguration.maxAge == expectedConfig.maxAge
        TestUtil.compareLists(casCorsConfiguration.exposedHeaders, expectedConfig.exposedHeaders)

        where:

        config        || expectedConfig
        null          || defaultConfig
        defaultConfig || defaultConfig
        testConfig1   || testConfig1
        testConfig2   || testConfig2
    }

    def "Test allowedOrigins regex pattern matching"() {
        when:

        def casCorsConfiguration = new CasCorsConfiguration(config)

        then:

        casCorsConfiguration.checkOrigin(origin) == expectedResult

        where:

        config      || origin                      || expectedResult
        null        || 'authorised.ala.org.au'     || 'authorised.ala.org.au'
        null        || 'unauthorised.org.au'       || 'unauthorised.org.au'
        null        || 'www.authorised.ala.org.au' || 'www.authorised.ala.org.au'
        testConfig1 || 'authorised.ala.org.au'     || 'authorised.ala.org.au'
        testConfig1 || 'unauthorised.org.au'       || null
        testConfig1 || 'www.authorised.ala.org.au' || null
        testConfig2 || 'authorised.ala.org.au'     || 'authorised.ala.org.au'
        testConfig2 || 'unauthorised.com.au'       || null
        testConfig2 || 'www.authorised.ala.org.au' || 'www.authorised.ala.org.au'
    }
}