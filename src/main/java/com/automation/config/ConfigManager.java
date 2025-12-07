package com.automation.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;

/**
 * Configuration interface using Owner library.
 * Automatically loads properties from config.properties file.
 */
@Config.Sources({
        "classpath:config.properties",
        "classpath:config-${env}.properties"
})
public interface ConfigManager extends Config {

    /**
     * Get the singleton instance of ConfigManager
     */
    static ConfigManager getInstance() {
        return ConfigCache.getOrCreate(ConfigManager.class);
    }

    @Key("base.url")
    @DefaultValue("https://jsonplaceholder.typicode.com")
    String baseUrl();

    @Key("api.timeout")
    @DefaultValue("30000")
    int timeout();

    @Key("retry.count")
    @DefaultValue("3")
    int retryCount();

    @Key("log.request")
    @DefaultValue("true")
    boolean logRequest();

    @Key("log.response")
    @DefaultValue("true")
    boolean logResponse();

    @Key("content.type")
    @DefaultValue("application/json")
    String contentType();

    @Key("auth.type")
    @DefaultValue("none")
    String authType();

    @Key("auth.token")
    String authToken();

    @Key("auth.username")
    String authUsername();

    @Key("auth.password")
    String authPassword();

    @Key("login.phone.number")
    String loginPhoneNumber();

    @Key("login.token")
    String loginToken();

    @Key("response.time.threshold")
    @DefaultValue("40000")
    int responseTimeThreshold();

    @Key("buyer.app.base.url")
    @DefaultValue("https://api.navofashion.in")
    String buyerAppBaseUrl();

    @Key("buyer.app.phone.number")
    String buyerAppPhoneNumber();

    @Key("buyer.app.token")
    @DefaultValue("000000")
    String buyerAppToken();
}
