package scanalesespinoza.webservice;

import org.jboss.resteasy.annotations.SseElementType;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/stream")
public class StreamResource {

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamEvents(@javax.ws.rs.core.Context SseEventSink eventSink, @javax.ws.rs.core.Context Sse sse) {
        try (SseEventSink sink = eventSink) {
            OutboundSseEvent event = sse.newEventBuilder()
                .name("status-update")
                .data(String.class, "Service is up")
                .build();
            sink.send(event);

            // Additional logic to handle ongoing updates or periodic checks
        } catch (Exception e) {
            // Handle exceptions
        }
    }
}
