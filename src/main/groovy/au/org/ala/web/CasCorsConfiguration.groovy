package au.org.ala.web

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.web.cors.CorsConfiguration

import java.util.regex.Pattern

/**
 * Extend CorsConfiguration to use regex matching of allowedOrigins.
 *
 * The default values; allowedMethods=ALL, other defaults set by CorsConfiguration.applyPermitDefaultValues()
 */
@CompileStatic
@InheritConstructors
class CasCorsConfiguration extends CorsConfiguration {

    List<Pattern> allowedOriginPatterns = new ArrayList<>()

    public CasCorsConfiguration(Map other) {
        super()

        setAllowedOrigins((List<String>) other?.allowedOrigins)
        setAllowCredentials((Boolean) other?.allowCredentials)
        setAllowedHeaders((List<String>) other?.allowedHeaders)
        setExposedHeaders((List<String>) other?.exposedHeaders)
        setMaxAge((Long) other?.maxAge)

        setAllowedMethods((List<String>) other?.allowedMethods)

        // allow all methods by default, same as grails.web.mapping.cors.GrailsDefaultCorsConfiguration
        if (this.allowedMethods == null) {
            this.setAllowedMethods(Arrays.asList(ALL))
        }

        applyPermitDefaultValues()

        syncAllowedOriginPatterns()
    }

    @Override
    String checkOrigin(String requestOrigin) {
        if (requestOrigin == null) {
            return null;
        }

        String origin = super.checkOrigin(requestOrigin)

        // do pattern matching when a match is not found by CorsConfiguration.checkOrigin
        if (origin == null) {
            List<Pattern> patterns = getAllowedOriginPatterns();
            for (Pattern allowedOriginPattern : patterns) {
                if (requestOrigin.matches(allowedOriginPattern)) {
                    return requestOrigin;
                }
            }
        }

        return origin
    }

    private void syncAllowedOriginPatterns() {
        List<Pattern> allowedOriginPatterns = new ArrayList<>()
        List<String> origins = getAllowedOrigins();
        if (origins != null) {
            for (String allowedOrigin : origins) {
                // cleanup configuration values that may be wrapped in whitespace
                String origin = allowedOrigin.trim()

                // wrap pattern to match the entire origin
                if (CorsConfiguration.ALL.equals(origin)) {
                    allowedOriginPatterns.add(Pattern.compile('^.*$'))
                } else {
                    String start = origin.startsWith('^') ? '' : '^'
                    String end = origin.endsWith('$') ? '' : '$'
                    allowedOriginPatterns.add(Pattern.compile(start + origin + end))
                }
            }
        }
        this.allowedOriginPatterns = allowedOriginPatterns
    }

    @Override
    void setAllowedOrigins(List<String> allowedOrigins) {
        super.setAllowedOrigins(allowedOrigins)
        syncAllowedOriginPatterns()
    }

    @Override
    void addAllowedOrigin(String origin) {
        super.addAllowedOrigin(origin)
        syncAllowedOriginPatterns()
    }
}
