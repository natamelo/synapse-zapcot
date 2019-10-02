import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;

import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test11CreateReactorSolicitation {

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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons11", "tester123", "ONS");
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

        ServiceUtil.wait(5);

    }

    @Test
    public void test01CreateValidSolicitationsWithReactor() {

        String access_token = ServiceUtil.doLogin("testerons11", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "REATOR", "MOS",
        "5", "550kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(2);

        payloadSolicitation.put("action", "DESLIGAR");
        payloadSolicitation.put("equipment", "REATOR");
        payloadSolicitation.put("substation", "ATI");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(2);
    }

    @Test
    public void test02createInvalidSolicitationWithCapacitor() {
        
        String access_token = ServiceUtil.doLogin("testerons11", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "REATOR", "MOS",
        "5", "", true, "CTEEP");
        
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Voltage value must be informed for 'REATOR'."));
        
        ServiceUtil.wait(5);

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("ELEVAR", "REATOR", "MOS",
        "5", "550kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid action for equipment type 'REATOR'."));
        
        ServiceUtil.wait(5);

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "REATOR", "MOS",
        "-5", "550kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid amount value for equipment type 'REATOR'."));
        
        ServiceUtil.wait(5);

    }

}
