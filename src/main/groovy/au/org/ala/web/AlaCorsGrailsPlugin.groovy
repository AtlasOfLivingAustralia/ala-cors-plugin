package au.org.ala.web

import grails.plugins.Plugin

class AlaCorsGrailsPlugin extends Plugin {

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

    CasCorsConfigurationSource casCorsConfigSource

    Closure doWithSpring() {
        { ->
            casCorsConfigSource = new CasCorsConfigurationSource(grailsApplication.config)
            alaCorsFilter(CasCorsFilter, casCorsConfigSource)
        }
    }

    @Override
    void onConfigChange(Map<String, Object> event) {
        super.onConfigChange(event)

        casCorsConfigSource.configure(grailsApplication.config)
    }
}
