package util;

import io.restassured.RestAssured;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;

public class OrderUtil {
    // Fill with the ID of the last solicitation created
    public static int LAST_SOLICITATION_ID = 1;

        public static void createSolicitationWithStatusNew() {
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

    public static void createSolicitationWithStatusCanceled() {
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

    public static void createSolicitationWithStatusAccepted() {
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

    public static void createSolicitationWithStatusExecuted() {
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

    public static void createSolicitationWithStatusBlocked() {
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

    public static void createSolicitationWithStatusRequired() {
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

    public static void createSolicitationWithStatusContested() {
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
