package scanalesespinoza.dataservice;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import scanalesespinoza.model.UserRepository;


@ApplicationScoped
public class DatabaseReconnector {

    @Inject
    UserRepository userRepository;

    @Scheduled(every="10s")
    @Transactional
    public void checkDatabaseConnection() {
        try {
            userRepository.find("SELECT 1").firstResult(); // Attempt to execute a simple operation
            System.out.println("WE ARE ONLINE!");
        } catch (Exception e) {
            System.out.println("WE ARE ONFFLINE :(");
        }
    }
}
