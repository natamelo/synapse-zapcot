import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test05ChangeControlSolicitationStatus {

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

    }

    @Test
    public void test01SolicitationStatusHappyPath() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "CAPACITOR", "MOS",
        "FASE", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));

        ServiceUtil.wait(5);

        payloadLogin = DataUtil.buildPayloadLogin("testercteep", "tester123");

        access_token = RestAssured.
                given().
                body(payloadLogin).
                when().
                post("login").
                then().
                statusCode(200).
                extract().path("access_token");

        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("AWARE");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/5").
                then().
                statusCode(200).
                body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "ANSWERED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/5").
                then().
                statusCode(200).
                body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test02SolicitationCanceled() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");
        
        ServiceUtil.wait(5);        

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "CAPACITOR", "MOS",
        "FASE", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));

        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CANCELED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/6").
                then().
                statusCode(200).
                body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test03InvalidStatusChange() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        ServiceUtil.wait(5);
        
        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "CAPACITOR", "MOS",
        "FASE", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));

        ServiceUtil.wait(5);

        payloadLogin = DataUtil.buildPayloadLogin("testercteep", "tester123");

        access_token = RestAssured.
                given().
                body(payloadLogin).
                when().
                post("login").
                then().
                statusCode(200).
                extract().path("access_token");

        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ANSWERED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/7").
                then().
                statusCode(400).
                body("error", equalTo("Inconsistent change of status."));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "RETURNED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/7").
                then().
                statusCode(400).
                body("error", equalTo("Inconsistent change of status."));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "AWARE");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/7").
                then().
                statusCode(200).
                body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "RETURNED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/7").
                then().
                statusCode(400).
                body("error", equalTo("Inconsistent change of status."));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "AWARE");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/7").
                then().
                statusCode(400).
                body("error", equalTo("Inconsistent change of status."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test04UnauthorizedStatusChange() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String ons_access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        ServiceUtil.wait(5);

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "CAPACITOR", "MOS",
        "FASE", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + ons_access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));

        ServiceUtil.wait(5);

        payloadLogin = DataUtil.buildPayloadLogin("testercteep", "tester123");

        String cteep_access_token = RestAssured.
                given().
                body(payloadLogin).
                when().
                post("login").
                then().
                statusCode(200).
                extract().path("access_token");
        
        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CANCELED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + cteep_access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/5").
                then().
                statusCode(401).
                body("soft_logout", equalTo("Not allowed for users from CTEEP"));

        ServiceUtil.wait(5);

        payloadChangeStatus.put("status", "AWARE");

        RestAssured.
                given().
                header("Authorization", "Bearer " + ons_access_token).
                body(payloadChangeStatus).
                when().
                put("voltage_control_solicitation/8").
                then().
                statusCode(401).
                body("soft_logout", equalTo("Not allowed for users from ONS"));

        ServiceUtil.wait(5);

    }

    //TODO: CHANGE STATUS WITH TIME EXPIRED
}
