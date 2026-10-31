package com.automation.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;

/**
 * Configuration interface using Owner library.
 * Precedence: -D system properties (CI env vars are copied into these by
 * getInstance), then the gitignored secrets.properties, then config.properties.
 * MERGE: every source is read; the first one that defines a key wins.
 */
@LoadPolicy(LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "classpath:secrets.properties",
        "classpath:config.properties",
        "classpath:config-${env}.properties"
})
public interface ConfigManager extends Config {

    /**
     * Get the singleton instance of ConfigManager
     */
    static ConfigManager getInstance() {
        applySecretEnvVars();
        return ConfigCache.getOrCreate(ConfigManager.class);
    }

    /**
     * CI supplies the login credentials as env vars. Copy each one into its
     * property key unless a -D system property already set it, so the
     * "system:properties" source picks it up. Runs before the config is cached.
     */
    static void applySecretEnvVars() {
        String[][] envToKey = {
                {"BOMB_LOGIN_PHONE", "login.phone.number"},
                {"BOMB_LOGIN_OTP", "login.token"},
                {"BUYER_LOGIN_PHONE", "buyer.app.phone.number"},
                {"BUYER_LOGIN_OTP", "buyer.app.token"},
        };
        for (String[] pair : envToKey) {
            String value = System.getenv(pair[0]);
            if (value != null && !value.isBlank() && System.getProperty(pair[1]) == null) {
                System.setProperty(pair[1], value);
            }
        }
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
    String buyerAppToken();

    // Headers the Navo app sends on every request
    @Key("buyer.app.version")
    @DefaultValue("4.1.3-debug")
    String buyerAppVersion();

    @Key("buyer.app.version.code")
    @DefaultValue("420")
    String buyerAppVersionCode();

    @Key("buyer.app.user.segment")
    @DefaultValue("2")
    String buyerAppUserSegment();

    @Key("buyer.app.zone")
    @DefaultValue("india")
    String buyerAppZone();

    @Key("buyer.app.language")
    @DefaultValue("en")
    String buyerAppLanguage();
}
