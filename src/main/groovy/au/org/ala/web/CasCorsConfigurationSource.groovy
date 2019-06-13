package au.org.ala.web

import au.org.ala.util.AuthenticationCookieUtils
import au.org.ala.util.PatternMatchingUtils
import org.springframework.http.HttpHeaders
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

/**
 * Provide CORS configuration depending on the CAS filter response that would occur.
 *
 * The intended purpose is to limit CORS to authorised origins when a CAS login occurs than
 * unauthorised origins. Other services that do not perform a CAS login will not be limited.
 *
 * This is disabled with:
 *   security.cors.enable = false
 *
 * CORS configuration is defined for authenticated services and unauthenticated services with the following maps.
 *   security.cors.authenticated
 *   security.cors.unauthenticated
 *
 * CORS parameters available for corsLoggedIn and corsOther:
 *   allowedOrigins       Array of Strings. Regex matching (Behaviour of '*' is not changed).
 *   allowedMethods       Array of Strings. Exact matching.
 *   allowCredentials     Boolean.
 *   allowedHeaders       Array of Strings. Exact matching.
 *   exposedHeaders       Array of Strings. Exact matching.
 *   maxAge               Long.
 *
 * The default values used are set by CorsConfiguration.applyPermitDefaultValues()
 *
 * Order of response rules:
 *   CAS Pattern Matched                      Authentication state    Origin    CORS configuration
 *   uriExcludeFilterPattern                  ANY                     ANY       corsOther
 *   uriFilterPattern                         ANY                     ALLOWED   corsLoggedIn
 *   uriFilterPattern                         ANY                     ANY       corsOther
 *   uriFilterAuthenticateIfLoggedInPattern   LOGGED IN               ALLOWED   corsLoggedIn
 *   uriFilterAuthenticateIfLoggedInPattern   LOGGED IN               ANY       corsOther
 *   uriFilterAuthenticateIfLoggedInPattern   NOT LOGGED IN           ANY       corsOther
 *
 * Example configuration:
 *   security.cas.authenticated.allowedOrigins = ['.*\.ala\.org\.au(:[0-9]+)?']
 *   security.cas.unauthenticated.allowedOrigins = ['*']
 *
 */
class CasCorsConfigurationSource implements CorsConfigurationSource {

    final Map loggedInConfigDefault = [allowedOrigins: ['.*\\.ala\\.org\\.au(:[0-9]+)?']]
    final Map otherConfigDefault = [allowedOrigins: ['*'], allowCredentials: false]

    List<Pattern> uriInclusionPatterns = new ArrayList<>()
    List<Pattern> authOnlyIfLoggedInPatterns = new ArrayList<>()
    List<Pattern> uriExclusionPatterns = new ArrayList<>()

    Boolean corsEnabled
    CorsConfiguration loggedInConfig
    CorsConfiguration otherConfig

    CasCorsConfigurationSource(Map config) {
        super()

        configure(config)
    }

    void configure(Map config) {
        def casConfig = config.security.cas
        def corsAuthenticated = config.security.cors.authenticated ?: loggedInConfigDefault
        def corsUnauthenticated = config.security.cors.unauthenticated ?: otherConfigDefault

        uriInclusionPatterns = PatternMatchingUtils.getPatternList(casConfig.contextPath ?: '', casConfig.uriFilterPattern);
        authOnlyIfLoggedInPatterns = PatternMatchingUtils.getPatternList(casConfig.contextPath ?: '', casConfig.authenticateOnlyIfLoggedInFilterPattern);
        uriExclusionPatterns = PatternMatchingUtils.getPatternList(casConfig.contextPath ?: '', casConfig.uriExclusionFilterPattern);

        corsEnabled = config.security.cors.enable ? config.security.cors.enable.toBoolean() : true
        loggedInConfig = new CasCorsConfiguration(corsAuthenticated)
        otherConfig = new CasCorsConfiguration(corsUnauthenticated)
    }

    @Override
    CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        if (corsEnabled) {
            String requestUri = ((HttpServletRequest) request).getRequestURI();
            if (PatternMatchingUtils.matches(requestUri, uriExclusionPatterns)) {
                return otherConfig
            } else if (PatternMatchingUtils.matches(requestUri, uriInclusionPatterns) &&
                    loggedInConfig.checkOrigin(request.getHeader(HttpHeaders.ORIGIN)) != null) {
                return loggedInConfig
            } else if (PatternMatchingUtils.matches(requestUri, authOnlyIfLoggedInPatterns) &&
                    AuthenticationCookieUtils.isUserLoggedIn((HttpServletRequest) request) &&
                    loggedInConfig.checkOrigin(request.getHeader(HttpHeaders.ORIGIN)) != null) {
                return loggedInConfig
            } else {
                return otherConfig
            }
        } else {
            return null
        }
    }
}
