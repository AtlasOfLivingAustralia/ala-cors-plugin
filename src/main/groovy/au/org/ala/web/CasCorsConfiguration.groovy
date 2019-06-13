package au.org.ala.web

import org.springframework.web.cors.CorsConfiguration

import java.util.regex.Pattern

/**
 * Extend CorsConfiguration to use regex matching of allowedOrigins.
 */
class CasCorsConfiguration extends CorsConfiguration {

    List<Pattern> allowedOriginPatterns = new ArrayList<>()

    public CasCorsConfiguration(Map other) {
        super()

        allowedOrigins = other.allowedOrigins ?: null
        allowCredentials = other.allowCredentials ?: null
        allowedHeaders = other.allowedHeaders ?: null
        exposedHeaders = other.exposedHeaders ?: null
        maxAge = other.maxAge ?: null

        setAllowedMethods(other.allowedMethods ?: null)

        applyPermitDefaultValues()

        syncAllowedOriginPatterns()
    }

    @Override
    String checkOrigin(String requestOrigin) {
        String origin = super.checkOrigin(requestOrigin)

        // do pattern matching when a match is not found by CorsConfiguration.checkOrigin
        if (origin == null) {
            for (Pattern allowedOriginPattern : this.allowedOriginPatterns) {
                if (requestOrigin.matches(allowedOriginPattern)) {
                    return requestOrigin;
                }
            }
        }

        return origin
    }

    private void syncAllowedOriginPatterns() {
        List<Pattern> allowedOriginPatterns = new ArrayList<>()
        if (allowedOrigins != null) {
            for (String allowedOrigin : allowedOrigins) {
                // wrap pattern to match the entire origin
                if (CorsConfiguration.ALL.equals(allowedOrigin)) {
                    allowedOriginPatterns.add(Pattern.compile('^.*$'))
                } else {
                    String start = allowedOrigin.startsWith('^') ? '' : '^'
                    String end = allowedOrigin.endsWith('$') ? '' : '$'
                    allowedOriginPatterns.add(Pattern.compile(start + allowedOrigin + end))
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
