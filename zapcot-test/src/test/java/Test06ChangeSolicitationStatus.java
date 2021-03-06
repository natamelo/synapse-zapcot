import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import util.DataUtil;
import util.ServiceUtil;


public class Test06ChangeSolicitationStatus {

    // Fill with the ID of the last solicitation created
    public static int LAST_SOLICITATION_ID = 1;

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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons06", "tester123", "ONS");
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

        // Create a CTEEP user
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep06", "tester123", "CTEEP");
        session = ServiceUtil.getSession(userCTEEP);
        userCTEEP.put("auth", ServiceUtil.getAuthObject(session));

        RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(userCTEEP).
                when().
                    post("register").
                then().
                    statusCode(200).
                    extract().path("user_id");

        ServiceUtil.wait(2);

    }

    @Test
    public void test01ChangeStatusToCanceled() {

        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CANCELED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));
    }


    @Test
    public void test02ChangeStatusToAccepted() {

        // NEW > ACCEPTED
        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(5);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));


        // REQUIRED > ACCEPTED (NEW > CONTESTED > REQUIRED > ACCEPTED)

        access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CONTESTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        access_token = ServiceUtil.doLogin("testerons06", "tester123");

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("REQUIRED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));


    }

    @Test
    public void test03ChangeStatusToExecuted() {

        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("EXECUTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));
    }

    @Test
    public void test04ChangeStatusToContested() {
        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CONTESTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

    }

    @Test
    public void test05ChangeStatusToRequired() {
        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CONTESTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        access_token = ServiceUtil.doLogin("testerons06", "tester123");

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("REQUIRED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));


    }

    @Test
    public void test06ChangeStatusToBlocked() {
        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("BLOCKED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        access_token = ServiceUtil.doLogin("testerons06", "tester123");

        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("TURN_ON", "REACTOR", "MOS",
                "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        access_token = ServiceUtil.doLogin("testercteep06", "tester123");

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ACCEPTED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus = DataUtil.buildPayloadChangeStatus("BLOCKED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/" + LAST_SOLICITATION_ID).
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);


    }
}