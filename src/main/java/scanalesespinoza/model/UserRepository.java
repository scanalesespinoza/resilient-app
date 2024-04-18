package scanalesespinoza.model;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import scanalesespinoza.ratelimitservice.RateLimiterService;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    @Inject
    RateLimiterService rateLimiter;  // Inject the rate limiting service
    
    public boolean checkConnection() {
        try {
            // This will throw if there is no database connection
            find("SELECT 1").firstResult();
            System.out.println("checkConnection OK");
            return true;
        } catch (PersistenceException e) {
            return false;
        }
    }

    @Override
    public List<User> listAll() {
        if (!rateLimiter.tryConsumeDb(1)) {
            throw new RuntimeException("Rate limit exceeded. Cannot retrieve users at this time.");
        }
        long startTime = System.currentTimeMillis();
        List<User> users = findAll().list();
        long responseTime = System.currentTimeMillis() - startTime;
    
        rateLimiter.recordResponseTime(responseTime);
    
        return users;
    }

}