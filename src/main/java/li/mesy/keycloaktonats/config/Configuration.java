package li.mesy.keycloaktonats.config;

import io.nats.client.Options;

import java.util.Optional;

/**
 * Represents our own configuration based on environment variables because the stone age where we used XML is over
 *
 * @author Lukas Schulte Pelkum
 * @version 0.1.0
 * @since 0.1.0
 */
public class Configuration {

    private final boolean useStreaming;
    private final String url;
    private final String streamingClusterId;
    private final String streamingClientId;

    private Configuration(
            final boolean useStreaming,
            final String url,
            final String streamingClusterId,
            final String streamingClientId
    ) {
        this.useStreaming = useStreaming;
        this.url = url;
        this.streamingClusterId = streamingClusterId;
        this.streamingClientId = streamingClientId;
    }

    /**
     * Loads the configuration using the systems environment variables
     *
     * @return The loaded configuration
     */
    public static Configuration loadFromEnv() {
        final boolean useStreaming = "true".equalsIgnoreCase(System.getenv("KEYCLOAK_NATS_STREAMING"));
        final String url = Optional.ofNullable(System.getenv("KEYCLOAK_NATS_URL")).orElse(Options.DEFAULT_URL);
        final String streamingClusterId = Optional.ofNullable(System.getenv("KEYCLOAK_NATS_STREAMING_CLUSTER_ID")).orElse("");
        final String streamingClientId = Optional.ofNullable(System.getenv("KEYCLOAK_NATS_STREAMING_CLIENT_ID")).orElse("");

        return new Configuration(
                useStreaming,
                url,
                streamingClusterId,
                streamingClientId
        );
    }

    public boolean useStreaming() {
        return this.useStreaming;
    }

    public String getUrl() {
        return this.url;
    }

    public String getStreamingClusterId() {
        return this.streamingClusterId;
    }

    public String getStreamingClientId() {
        return this.streamingClientId;
    }

}
