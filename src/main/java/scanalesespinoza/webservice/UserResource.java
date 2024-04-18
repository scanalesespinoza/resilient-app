package scanalesespinoza.webservice;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import scanalesespinoza.model.User;
import scanalesespinoza.model.UserRepository;
import scanalesespinoza.ratelimitservice.RateLimiterService;


@Path("/users")
public class UserResource {

    @Inject
    UserRepository userRepository;

    @Inject
    RateLimiterService rateLimiter;
    
    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkConnection() {
        if (userRepository.checkConnection()) {
            return Response.ok("Hello from resilient database!").build();
        } else {
            ErrorResponse errorResponse = new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                                                            "Database is currently unavailable. Please try again later.");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                           .entity(errorResponse).build();
        }
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        if (!rateLimiter.tryConsumeWs(1)) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                           .entity("Rate limit exceeded").build();
        }
        try {
            if (userRepository.checkConnection()) {
                List<User> users = userRepository.listAll();
                return Response.ok(users).build();
            } else {
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                                                                "Database is currently unavailable. Please try again later.");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                               .entity(errorResponse).build();
            }    
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                                            "An error occurred while retrieving users.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(errorResponse).build();
        }
    }
}