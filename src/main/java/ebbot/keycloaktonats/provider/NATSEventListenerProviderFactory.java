package ebbot.keycloaktonats.provider;

import io.nats.client.*;
import io.nats.client.api.StreamConfiguration;
import ebbot.keycloaktonats.config.Configuration;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class NatsConnectionListener implements ConnectionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NatsConnectionListener.class);
    public void connectionEvent(Connection natsConnection, Events event) {
        LOGGER.debug("NATS Connection Status: {}", event.toString());
    }
}
/**
 * Provides the {@link NATSEventListenerProvider} or {@link NOOPEventListenerProvider} to Keycloak
 *
 * @author Lukas Schulte Pelkum
 * @version 0.1.0
 * @since 0.1.0
 */
public class NATSEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(NATSEventListenerProviderFactory.class);

    private EventListenerProvider listener;

    @Override
    public EventListenerProvider create(final KeycloakSession session) {
        return this.listener;
    }

    @Override
    public void init(final Config.Scope unusedConfig) {
        // We use our own configuration as I don't want to mess around with XML from two thousand years ago
        final Configuration config = Configuration.loadFromEnv();

        try {
            Options options = new Options.Builder()
            .server(config.getUrl())
                .connectionListener(new NatsConnectionListener())
            .build();
            Connection natsConnection = Nats.connect(options);

            if (config.useJetStream()) {
                // Use JetStream connection
                buildAdminEventStream(natsConnection, config);
                buildClientEventStream(natsConnection, config);

                this.listener = new NATSEventListenerProvider(natsConnection.jetStream(), natsConnection);
            } else {
                // Use classic connection
                this.listener = new NATSEventListenerProvider(null, natsConnection);
            }

        } catch (final IOException | InterruptedException | JetStreamApiException exception) {
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
        } catch (final IOException | InterruptedException exception) {
            LOGGER.error("could not close NATS connection", exception);
        }
    }

    @Override
    public String getId() {
        return "keycloak-nats-adapter";
    }

    private void buildAdminEventStream(Connection natsConnection, Configuration config) throws IOException, JetStreamApiException {
        final String streamName = "keycloak-admin-event-stream";
        JetStream jetStream = natsConnection.jetStream();
        StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                .name(streamName)
                .subjects("keycloak.event.admin.>")
                .maxBytes(config.getJetStreamAdminSize() * 1024 * 1024)
                .build();

        try {
            jetStream.getStreamContext(streamName);
            natsConnection.jetStreamManagement().updateStream(streamConfiguration);
        } catch (JetStreamApiException e) {
            natsConnection.jetStreamManagement().addStream(streamConfiguration);
        }
    }

    private void buildClientEventStream(Connection natsConnection, Configuration config) throws IOException, JetStreamApiException {
        final String streamName = "keycloak-client-event-stream";
        JetStream jetStream = natsConnection.jetStream();
        StreamConfiguration streamConfiguration = StreamConfiguration.builder()
                .name(streamName)
                .subjects("keycloak.event.client.>")
                .maxBytes(config.getJetStreamClientSize() * 1024 * 1024)
                .build();

        try {
            jetStream.getStreamContext(streamName);
            natsConnection.jetStreamManagement().updateStream(streamConfiguration);
        } catch (JetStreamApiException e) {
            natsConnection.jetStreamManagement().addStream(streamConfiguration);
        }
    }

}
