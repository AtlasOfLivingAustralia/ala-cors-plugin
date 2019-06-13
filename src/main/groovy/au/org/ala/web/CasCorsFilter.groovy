package au.org.ala.web

import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * CorsFilter with Ordered.HIGHEST_PRECEDENCE
 */
class CasCorsFilter extends CorsFilter implements Ordered {

    CasCorsFilter(CorsConfigurationSource source) {
        super(source)
    }

    @Override
    int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE
    }
}
