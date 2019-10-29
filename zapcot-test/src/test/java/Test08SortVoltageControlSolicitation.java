import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test08SortVoltageControlSolicitation {

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

        // Create a CTEEP user
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep08", "tester123", "CTEEP");
        String session = ServiceUtil.getSession(userCTEEP);
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

        // Create a ONS user
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons08", "tester123", "ONS");
        session = ServiceUtil.getSession(userONS);
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

    /*
        Order By Status and Timestamp
        Group 1: 'BLOCKED', 'CONTESTED', 'NEW', 'REQUIRED' (Timestamp ASC)
        Group 2: 'LATE', 'ACCEPTED' (Timestamp ASC)
        Group 3: 'EXECUTED', 'CANCELED' (Timestamp DESC)
    */

    @Test
    public void test1ONSSorting() {

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

        createSolicitationWithStatusNew();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW"));

        createSolicitationWithStatusCanceled();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("CANCELED"));

        createSolicitationWithStatusAccepted();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("ACCEPTED")).
                    body("get(2).events[0].status", equalTo("CANCELED"));

        createSolicitationWithStatusExecuted();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("ACCEPTED")).
                    body("get(2).events[0].status", equalTo("EXECUTED")).
                    body("get(3).events[0].status", equalTo("CANCELED"));

        createSolicitationWithStatusBlocked();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("BLOCKED")).
                    body("get(2).events[0].status", equalTo("ACCEPTED")).
                    body("get(3).events[0].status", equalTo("EXECUTED")).
                    body("get(4).events[0].status", equalTo("CANCELED"));

        createSolicitationWithStatusRequired();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("BLOCKED")).
                    body("get(2).events[0].status", equalTo("REQUIRED")).
                    body("get(3).events[0].status", equalTo("ACCEPTED")).
                    body("get(4).events[0].status", equalTo("EXECUTED")).
                    body("get(5).events[0].status", equalTo("CANCELED"));

        createSolicitationWithStatusContested();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("BLOCKED")).
                    body("get(2).events[0].status", equalTo("REQUIRED")).
                    body("get(3).events[0].status", equalTo("CONTESTED")).
                    body("get(4).events[0].status", equalTo("ACCEPTED")).
                    body("get(5).events[0].status", equalTo("EXECUTED")).
                    body("get(6).events[0].status", equalTo("CANCELED"));

    }

    private void createSolicitationWithStatusNew() {
        LAST_SOLICITATION_ID += 1;

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

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
    }

    private void createSolicitationWithStatusCanceled() {
        LAST_SOLICITATION_ID += 1;

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

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

    private void createSolicitationWithStatusAccepted() {
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

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

        access_token = ServiceUtil.doLogin("testercteep08", "tester123");

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
    }

    private void createSolicitationWithStatusExecuted() {
                String access_token = ServiceUtil.doLogin("testerons08", "tester123");
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

        access_token = ServiceUtil.doLogin("testercteep08", "tester123");

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

    private void createSolicitationWithStatusBlocked() {
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

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

        access_token = ServiceUtil.doLogin("testercteep08", "tester123");

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

    }

    private void createSolicitationWithStatusRequired() {
                String access_token = ServiceUtil.doLogin("testerons08", "tester123");
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

        access_token = ServiceUtil.doLogin("testercteep08", "tester123");

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

        access_token = ServiceUtil.doLogin("testerons08", "tester123");

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

    private void createSolicitationWithStatusContested() {
        LAST_SOLICITATION_ID = LAST_SOLICITATION_ID + 1;

        String access_token = ServiceUtil.doLogin("testerons08", "tester123");

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

        access_token = ServiceUtil.doLogin("testercteep08", "tester123");

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

}
