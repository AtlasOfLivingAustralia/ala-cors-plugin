package au.org.ala.web

import grails.plugins.Plugin
import grails.util.Holders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered
import org.springframework.web.filter.CorsFilter

import javax.servlet.DispatcherType

class AlaCorsGrailsPlugin extends Plugin {

    private final static Logger logger = LoggerFactory.getLogger(AlaCorsGrailsPlugin.class)

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.2.11 >= *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = []

    def title = "ALA CORS Plugin" // Headline display name of the plugin
    def description = '''\
This plugin provides CORS services for use with ala-auth-plugin.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/AtlasOfLivingAustralia/ala-cors-plugin"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "MPL2"

    // Details of company behind the plugin (if there is one)
    def organization = [name: "Atlas of Living Australia", url: "http://www.ala.org.au/"]

    // Any additional developers beyond the author specified above.
    def developers = []

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "github", url: "https://github.com/AtlasOfLivingAustralia/ala-cors-plugin/issues"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/AtlasOfLivingAustralia/ala-cors-plugin"]

    // Maintain a reference to the alaCorsFilter config source because it is not accessible from the bean.
    // servletContext is available in doWithApplicationContext() where it is used to update casCorsConfigSource
    CasCorsConfigurationSource casCorsConfigSource = new CasCorsConfigurationSource(null, null)

    Closure doWithSpring() {
        { ->
            alaCorsFilter(FilterRegistrationBean) {
                name = 'ALA Cors Filter'
                filter = bean(CorsFilter, casCorsConfigSource)
                dispatcherTypes = EnumSet.of(DispatcherType.REQUEST)
                order = Ordered.HIGHEST_PRECEDENCE
                urlPatterns = ['/*']
                asyncSupported = true
            }
        }
    }

    void doWithApplicationContext() {
        if (!casCorsConfigSource.configure(Holders.config, Holders.servletContext)) {
            logger.error("alaCorsFilter is not configured. Missing config or servletContext.");
        }
    }

    @Override
    void onConfigChange(Map<String, Object> event) {
        super.onConfigChange(event)

        casCorsConfigSource.configure(Holders.config, Holders.servletContext)
    }
}
