package ebbot.keycloaktonats.provider;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

/**
 * Does nothing as I couldn't find a way of cancelling event listener provider creation with an error
 *
 * @author Lukas Schulte Pelkum
 * @version 0.1.0
 * @since 0.1.0
 */
public class NOOPEventListenerProvider implements EventListenerProvider {

    @Override
    public void onEvent(final Event event) {
    }

    @Override
    public void onEvent(final AdminEvent event, final boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }

}
