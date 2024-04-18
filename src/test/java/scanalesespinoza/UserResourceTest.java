package scanalesespinoza;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class UserResourceTest {
    @Test
    void testLiveEndpoint() {
        given()
          .when().get("/users/check")
          .then()
             .statusCode(200)
             .body(is("Hello from resilient database!"));
    }

}