import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;
import static org.hamcrest.CoreMatchers.equalTo;


public class Test09GetSubstations {

    @BeforeClass
    public static void setup() {
        String port = System.getProperty("server.port");
        if (port == null) {
            RestAssured.port = Integer.valueOf(8008);
        } else {
            RestAssured.port = Integer.valueOf(port);
        }

        String basePath = System.getProperty("server.base");
        if (basePath == null) {
            basePath = "/_matrix/client/r0/";
        }
        RestAssured.basePath = basePath;

        String baseHost = System.getProperty("server.host");
        if (baseHost == null) {
            baseHost = "http://localhost";
        }
        RestAssured.baseURI = baseHost;

        // Create a ONS user
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons09", "tester123", "ONS");
        String session = ServiceUtil.getSession(userONS);
        userONS.put("auth", ServiceUtil.getAuthObject(session));

        RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(userONS).
                when().
                    post("register").
                then().
                    statusCode(200).
                    extract().path("user_id");

        ServiceUtil.wait(2);

    }

    @Test
    public void test01SortByCreationTime() {

        String onsAccessToken = ServiceUtil.doLogin("testerons09", "tester123");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("substations").
                then().
                    statusCode(200).
                    body("get(0).code", equalTo("PIR")).
                    body("get(0).name", equalTo("Piratininga II")).
                    body("get(1).code", equalTo("MIR")).
                    body("get(1).name", equalTo("Mirassol II")).
                    body("get(2).code", equalTo("ATI")).
                    body("get(2).name", equalTo("Atibaia II")).
                    body("get(3).code", equalTo("MOS")).
                    body("get(3).name", equalTo("Mosque")).
                    body("get(4).code", equalTo("SAL")).
                    body("get(4).name", equalTo("Salto")).
                    body("get(5).code", equalTo("TES")).
                    body("get(5).name", equalTo("Teste"));

        ServiceUtil.wait(5);

    }
}
