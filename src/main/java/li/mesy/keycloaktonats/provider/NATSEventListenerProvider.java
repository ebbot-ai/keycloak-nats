package li.mesy.keycloaktonats.provider;

import org.keycloak.util.JsonSerialization;
import io.nats.client.Connection;
import io.nats.streaming.StreamingConnection;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Publishes incoming events to a NATS (Streaming) connection
 *
 * @author Lukas Schulte Pelkum
 * @version 0.1.0
 * @since 0.1.0
 */
public class NATSEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NATSEventListenerProvider.class);

    private final StreamingConnection streamingConnection;
    private final Connection plainConnection;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::publishAdminEvent, this::publishEvent);

    NATSEventListenerProvider(final StreamingConnection streamingConnection, final Connection plainConnection) {
        this.streamingConnection = streamingConnection;
        this.plainConnection = plainConnection;
    }

    @Override
    public void onEvent(Event event) {
        tx.addEvent(event.clone());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        tx.addAdminEvent(event, includeRepresentation);
    }

    @Override
    public void close() {
        // We re-use this object so we don't care about this methoprivate void publishAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {d
        // To close the connection we use NATSEventListenerProvider#closeConnection instead
    }

    private void publishEvent(Event event) {
        String serialized = this.serialize(event);
        String key = this.buildKey(event);
        this.send(key, serialized);
    }
    private void publishAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        String serialized = this.serialize(adminEvent);
        String key = this.buildKey(adminEvent);
        this.send(key, serialized);
    }

    private void send(String key, String value) {
        if (this.streamingConnection != null) {
            try {
                this.streamingConnection.publish(key, value.getBytes(StandardCharsets.UTF_8));
            } catch (final IOException | InterruptedException | TimeoutException exception) {
                LOGGER.error("could not send message to NATS Streaming (STAN)", exception);
            }
        }
        if (this.plainConnection != null) {
            this.plainConnection.publish(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String serialize(Object object) {
        try {
            return JsonSerialization.writeValueAsPrettyString(object);

        } catch (Exception e) {
          LOGGER.error("Could not serialize to JSON", e);
        }
        return "unparseable";
    }

    private String buildKey(final Event event) {
        // keycloak.event.client.<realm>.<result>.<clientId>.<type>
        return this.normalizeKey(String.format(
                "keycloak.event.client.%s.%s.%s.%s",
                event.getRealmId().replace(".", ""),
                event.getError() != null ? "error" : "success",
                event.getClientId().replace(".", ""),
                event.getType().toString().toLowerCase()
        ));
    }

    private String buildKey(final AdminEvent event) {
        // keycloak.event.admin.<realm>.<result>.<resourceType>.<operation>
        return this.normalizeKey(String.format(
                "keycloak.event.admin.%s.%s.%s.%s",
                event.getRealmId().replace(".", ""),
                event.getError() != null ? "error" : "success",
                event.getResourceTypeAsString().toLowerCase(),
                event.getOperationType().toString().toLowerCase()
        ));
    }

    private String normalizeKey(final String key) {
        // Remove everything except 'a-z', 'A-Z', '0-9', ' ', '_', '.' and '-' and replace spaces with underscores
        return key.replaceAll("[^a-zA-Z0-9 _.-]", "").
                replace(" ", "_");
    }

    void closeConnection() throws IOException, InterruptedException, TimeoutException {
        if (this.streamingConnection != null) {
            this.streamingConnection.close();
        }
        if (this.plainConnection != null) {
            this.plainConnection.close();
        }
    }

}
