package li.mesy.keycloaktonats.provider;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.streaming.NatsStreaming;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import li.mesy.keycloaktonats.config.Configuration;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Provides the {@link NATSEventListenerProvider} or {@link NOOPEventListenerProvider} to Keycloak
 *
 * @author Lukas Schulte Pelkum
 * @version 0.1.0
 * @since 0.1.0
 */
public class NATSEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NATSEventListenerProviderFactory.class);

    // We use ONE instance here as I don't see the point in creating new ones for every single event
    private EventListenerProvider listener;

    @Override
    public EventListenerProvider create(final KeycloakSession session) {
        return this.listener;
    }

    @Override
    public void init(final Config.Scope unusedConfig) {
        // We use our own configuration as I don't want to mess around with XML from two thousand years ago
        final Configuration config = Configuration.loadFromEnv();

        // Set up a streaming connection when configured
        if (config.useStreaming()) {
            try {
                final StreamingConnection connection = NatsStreaming.connect(
                        config.getStreamingClusterId(),
                        config.getStreamingClientId(),
                        new Options.Builder().natsUrl(config.getUrl()).build()
                );
                this.listener = new NATSEventListenerProvider(connection, null);
            } catch (final IOException | InterruptedException exception) {
                LOGGER.error("could not open NATS Streaming (STAN) connection", exception);
                this.listener = new NOOPEventListenerProvider();
            }
            return;
        }

        // Set up a plain NATS connection otherwise
        try {
            final Connection connection = Nats.connect(config.getUrl());
            this.listener = new NATSEventListenerProvider(null, connection);
        } catch (final IOException | InterruptedException exception) {
            LOGGER.error("could not open NATS connection", exception);
            this.listener = new NOOPEventListenerProvider();
        }
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        // Close the NATS connection of the NATS event adapter
        if (!(this.listener instanceof NATSEventListenerProvider)) {
            return;
        }
        try {
            ((NATSEventListenerProvider) this.listener).closeConnection();
        } catch (final IOException | InterruptedException | TimeoutException exception) {
            LOGGER.error("could not close NATS connection", exception);
        }
    }

    @Override
    public String getId() {
        return "keycloak-nats-adapter";
    }

}
