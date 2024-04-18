package scanalesespinoza.dataservice;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;

@Readiness
@ApplicationScoped
public class DatabaseConnectionHealthCheck implements HealthCheck {

    private final DataSource dataSource;

    public DatabaseConnectionHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {  // Timeout in seconds
                return HealthCheckResponse.up("Database connection");
            }
        } catch (Exception e) {
            // Log the exception details here
        }
        return HealthCheckResponse.down("Database connection");
    }
}