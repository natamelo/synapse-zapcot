import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;

import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test13CreateVoltageSynchronousSolicitation {

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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons13", "tester123", "ONS");
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
    public void test01CreateValidSolicitationsWithSynchronous() {

        String access_token = ServiceUtil.doLogin("testerons13", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("MAXIMIZE", "SYNCHRONOUS", "MOS",
        "5", "", null, "CTEEP");

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

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("RESET", "SYNCHRONOUS", "MOS",
        "", "", null, "CTEEP");

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

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("ADJUST", "SYNCHRONOUS", "MOS",
        "5", "", null, "CTEEP");

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

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("MINIMIZE", "SYNCHRONOUS", "MOS",
                "5", "", null, "CTEEP");

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
        
        String access_token = ServiceUtil.doLogin("testerons13", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("ADJUST", "SYNCHRONOUS", "MOS",
        "", "", null, "CTEEP");
        
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid amount value for equipment type 'SYNCHRONOUS'."));
        
        ServiceUtil.wait(5);

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "SYNCHRONOUS", "MOS",
        "", "", null, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid action for equipment type 'SYNCHRONOUS'."));
        
        ServiceUtil.wait(5);

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("ADJUST", "SYNCHRONOUS", "MOS",
        "-151", "", null, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid amount value for equipment type 'SYNCHRONOUS'."));
        
        ServiceUtil.wait(5);
    }

}
