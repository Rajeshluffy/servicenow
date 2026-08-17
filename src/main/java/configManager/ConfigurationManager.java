package configManager;

import java.util.Map;
import java.util.Properties;

import org.aeonbits.owner.ConfigCache;

public class ConfigurationManager {

    // Maps property keys (as used in code) to their environment variable names.
    // Env vars take precedence over anything in the .properties file.
    private static final Map<String, String> ENV_CREDENTIAL_MAPPINGS = Map.of(
        "service.now.username",      "SERVICE_NOW_USERNAME",
        "service.now.password",      "SERVICE_NOW_PASSWORD",
        "service.now.client.id",     "SERVICE_NOW_CLIENT_ID",
        "service.now.client.secret", "SERVICE_NOW_CLIENT_SECRET"
    );

    public static Configuration configuration() {
        Properties overrides = new Properties();
        ENV_CREDENTIAL_MAPPINGS.forEach((propKey, envKey) -> {
            String value = System.getenv(envKey);
            if (value != null && !value.isBlank()) {
                overrides.setProperty(propKey, value);
            }
        });
        return ConfigCache.getOrCreate(Configuration.class, overrides);
    }

    private ConfigurationManager() {}
}
