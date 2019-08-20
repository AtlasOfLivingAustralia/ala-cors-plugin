package au.org.ala.web

import au.org.ala.cas.client.UriFilter
import au.org.ala.cas.util.AuthenticationCookieUtils
import au.org.ala.cas.util.PatternMatchingUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource

import javax.servlet.ServletContext
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
 *
 * Default configuration:
 *   security.cas.authenticated.allowedOrigins = ['.*\.ala\.org\.au(:[0-9]+)?']
 *   security.cas.unauthenticated.allowedOrigins = ['*']
 *   security.cas.unauthenticated.allowCredentials = false
 *
 * Remaining default values; allowedMethods=ALL, other defaults set by CorsConfiguration.applyPermitDefaultValues()
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
 */
class CasCorsConfigurationSource implements CorsConfigurationSource {

    private final static Logger logger = LoggerFactory.getLogger(CasCorsConfigurationSource.class)

    final static Map loggedInConfigDefault = [allowedOrigins: ['.*\\.ala\\.org\\.au(:[0-9]+)?']]
    final static Map otherConfigDefault = [allowedOrigins: ['*'], allowCredentials: false]

    String contextPath
    List<Pattern> uriInclusionPatterns = new ArrayList<>()
    List<Pattern> authOnlyIfLoggedInPatterns = new ArrayList<>()
    List<Pattern> uriExclusionPatterns = new ArrayList<>()

    Boolean corsEnabled
    CorsConfiguration loggedInConfig
    CorsConfiguration otherConfig

    CasCorsConfigurationSource(Map config, ServletContext servletContext) {
        super()

        configure(config, servletContext)
    }

    boolean configure(Map config, ServletContext servletContext) {
        if (config == null || servletContext == null) {
            return false
        }

        def casConfig = config?.security?.cas
        def corsAuthenticated = config?.security?.cors?.authenticated ?: loggedInConfigDefault
        def corsUnauthenticated = config?.security?.cors?.unauthenticated ?: otherConfigDefault

        getCasConfig(servletContext)

        corsEnabled = config?.security?.cors?.enable ? config.security.cors.enable?.asBoolean() : true
        loggedInConfig = new CasCorsConfiguration(corsAuthenticated)
        otherConfig = new CasCorsConfiguration(corsUnauthenticated)

        return true
    }

    /**
     * Get CAS configuration parameters.
     *
     * Identical to ala-cas-client.
     *
     * @param servletContext
     */
    private void getCasConfig(ServletContext servletContext) {
        //
        // Get contextPath parameter
        //

        this.contextPath = servletContext.getInitParameter("contextPath");
        if (this.contextPath == null) {
            this.contextPath = servletContext.getContextPath();
            logger.debug("Using ServletContext contextPath: {}", this.contextPath);
        } else {
            logger.warn("Overriding ServletContext contextPath: {} with ServletContext init-param value: {}", servletContext.getContextPath(), this.contextPath);
        }

        //
        // Get URI inclusion filter patterns
        //
        String includedUrlPattern = servletContext.getInitParameter(UriFilter.URI_FILTER_PATTERN);
        if (includedUrlPattern == null) {
            includedUrlPattern = "";
        }
        logger.debug("Included URI Pattern = '" + includedUrlPattern + "'");
        this.uriInclusionPatterns = au.org.ala.cas.util.PatternMatchingUtils.getPatternList(contextPath, includedUrlPattern);

        //
        // Get URI exclusion filter patterns
        //
        String excludedUrlPattern = servletContext.getInitParameter(UriFilter.URI_EXCLUSION_FILTER_PATTERN);
        if (excludedUrlPattern == null) {
            excludedUrlPattern = "";
        }
        logger.debug("Excluded URI Pattern = '{}'", excludedUrlPattern);
        this.uriExclusionPatterns = au.org.ala.cas.util.PatternMatchingUtils.getPatternList(contextPath, excludedUrlPattern);

        //
        // Get Authenticate Only if Logged in filter patterns
        //
        String authOnlyIfLoggedInPattern = servletContext.getInitParameter(UriFilter.AUTHENTICATE_ONLY_IF_LOGGED_IN_FILTER_PATTERN);
        if (authOnlyIfLoggedInPattern == null) {
            authOnlyIfLoggedInPattern = "";
        }
        logger.debug("Authenticate Only if Logged in Pattern = '{}'", authOnlyIfLoggedInPattern);
        this.authOnlyIfLoggedInPatterns = au.org.ala.cas.util.PatternMatchingUtils.getPatternList(contextPath, authOnlyIfLoggedInPattern);
    }

    @Override
    CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        if (corsEnabled) {
            String requestUri = request.getRequestURI();
            if (PatternMatchingUtils.matches(requestUri, uriExclusionPatterns)) {
                return otherConfig
            } else if (PatternMatchingUtils.matches(requestUri, uriInclusionPatterns) &&
                    loggedInConfig.checkOrigin(request.getHeader(HttpHeaders.ORIGIN)) != null) {
                return loggedInConfig
            } else if (PatternMatchingUtils.matches(requestUri, authOnlyIfLoggedInPatterns) &&
                    AuthenticationCookieUtils.isUserLoggedIn(request) &&
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
