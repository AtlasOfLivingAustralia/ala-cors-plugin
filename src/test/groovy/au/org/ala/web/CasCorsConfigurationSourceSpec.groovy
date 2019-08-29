package au.org.ala.web

import org.springframework.http.HttpHeaders
import spock.lang.Specification

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest


class CasCorsConfigurationSourceSpec extends Specification {

    final static def testCasConfig = [
            security: [
                    cas: [
                            uriFilterPattern               : '/filtered*',
                            excludeUriFilterPattern        : '/excluded.*',
                            authOnlyIfLoggedInFilterPattern: '/onlyifloggedin.*',
                            contextPath                    : 'authorised.ala.org.au'
                    ]
            ]
    ]

    final static def testCorsConfig = [
            security: [
                    cors: [
                            authorised  : [
                                    allowedOrigins: ['.*\\.org\\.au(:[0-9]+)?', 'authorised.ala.org.au']
                            ],
                            unauthorised: [
                                    allowedOrigins  : ['*'],
                                    allowCredentials: false
                            ]
                    ]
            ]
    ]

    final static def testConfig = testCasConfig + testCorsConfig

    final static
    def defaultLoggedInConfig = [allowedOrigins: CasCorsConfigurationSource.loggedInConfigDefault.allowedOrigins, allowedMethods: null, allowedHeaders: null, allowCredentials: true, maxAge: null, exposedHeaders: null]
    final static
    def defaultOtherConfig = [allowedOrigins: CasCorsConfigurationSource.otherConfigDefault.allowedOrigins, allowedMethods: null, allowedHeaders: null, allowCredentials: true, maxAge: null, exposedHeaders: null]

    final static
    def testLoggedInConfig = [allowedOrigins: CasCorsConfigurationSource.loggedInConfigDefault.allowedOrigins, allowedMethods: null, allowedHeaders: null, allowCredentials: true, maxAge: null, exposedHeaders: null]
    final static
    def testOtherConfig = [allowedOrigins: CasCorsConfigurationSource.otherConfigDefault.allowedOrigins, allowedMethods: null, allowedHeaders: null, allowCredentials: true, maxAge: null, exposedHeaders: null]

    def "Test CasCorsConfiguration returned by CasCorsConfigurationSource.getCorsConfiguration"() {
        when:

        def casCorsConfigurationSource = new CasCorsConfigurationSource(config, buildServletContext())

        def matchedConfig = casCorsConfigurationSource.getCorsConfiguration(buildRequest(requestUri))

        then:

        if (resultIsLoggedInConfig) {
            matchedConfig == casCorsConfigurationSource.loggedInConfig
        } else {
            matchedConfig == casCorsConfigurationSource.otherConfig
        }

        where:

        config     || requestUri                             || resultIsLoggedInConfig
        testConfig || "authorised.ala.org.au/filtered"       || true
        testConfig || "unauthorised.org.au/filtered"         || false
        testConfig || "authorised.ala.org.au/excluded"       || false
        testConfig || "unauthorised.org.au/excluded"         || false
        testConfig || "authorised.ala.org.au/onlyifloggedin" || true
        testConfig || "unauthorised.org.au/onlyifloggedin"   || false

    }

    def "Test default CasCorsConfigurations returned by CasCorsConfigurationSource.getCorsConfiguration"() {
        when:

        def casCorsConfigurationSource = new CasCorsConfigurationSource(config, buildServletContext())

        def matchedConfig = casCorsConfigurationSource.getCorsConfiguration(buildRequest(requestUri, []))

        then:

        TestUtil.compareLists(matchedConfig.allowedOrigins, expectedConfig.allowedOrigins)

        where:

        config        || requestUri                       || expectedConfig
        testCasConfig || "authorised.ala.org.au/filtered" || defaultLoggedInConfig
        testCasConfig || "unauthorised.org.au/filtered"   || defaultOtherConfig
        testConfig    || "authorised.ala.org.au/filtered" || testLoggedInConfig
        testConfig    || "unauthorised.org.au/filtered"   || testOtherConfig
    }

    def buildRequest(requestUri) {
        def request1 = Mock(HttpServletRequest);
        request1.getRequestURI() >> {
            return requestUri
        }
        request1.getHeader(_) >> { key ->
            if (key.get(0) == HttpHeaders.ORIGIN) {
                def uriEnd = requestUri.indexOf('/')
                if (uriEnd < 0) uriEnd = requestUri.length()
                return requestUri.substring(0, uriEnd)
            } else {
                return null
            }
        }
        return request1
    }

    def buildServletContext() {
        def mockServletContext = Mock(ServletContext)
        mockServletContext.getInitParameter(_) >> { key ->
            return CasCorsConfigurationSourceSpec.testCasConfig.security.cas.get(key.get(0))
        }
        return mockServletContext
    }
}