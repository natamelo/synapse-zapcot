import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test06ChangeControlSolicitationStatus {

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
    public void test01SolicitationStatusHappyPath() {

        String access_token = ServiceUtil.doLogin("testerons06", "tester123");

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
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

        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("AWARE");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/2").
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
                    put("voltage_control_solicitation/2").
                then().
                    statusCode(200).
                    body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test02SolicitationCanceled() {

        String access_token = ServiceUtil.doLogin("testerons06", "tester123");
        
        ServiceUtil.wait(5);        

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
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
                put("voltage_control_solicitation/3").
                then().
                statusCode(200).
                body("message", equalTo("Solicitation status changed."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test03InconsistentStatusChange() {

        String access_token = ServiceUtil.doLogin("testerons06", "tester123");

        ServiceUtil.wait(5);
        
        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
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
        
        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("ANSWERED");

        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadChangeStatus)
                .when().
                put("voltage_control_solicitation/4").
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
                put("voltage_control_solicitation/4").
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
                put("voltage_control_solicitation/4").
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
                put("voltage_control_solicitation/4").
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
                put("voltage_control_solicitation/4").
                then().
                statusCode(400).
                body("error", equalTo("Inconsistent change of status."));

        ServiceUtil.wait(5);
    }

    @Test
    public void test04UnauthorizedStatusChange() {

        String ons_access_token = ServiceUtil.doLogin("testerons06", "tester123");

        ServiceUtil.wait(5);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
        "5", "500kV", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + ons_access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

        ServiceUtil.wait(20);

        String cteep_access_token = ServiceUtil.doLogin("testercteep06", "tester123");
        
        ServiceUtil.wait(5);

        Map<String, String> payloadChangeStatus = DataUtil.buildPayloadChangeStatus("CANCELED");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteep_access_token).
                    body(payloadChangeStatus)
                .when().
                    put("voltage_control_solicitation/2").
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
                    put("voltage_control_solicitation/5").
                then().
                    statusCode(401).
                    body("soft_logout", equalTo("Not allowed for users from ONS"));

        ServiceUtil.wait(5);

    }

    //TODO: CHANGE STATUS WITH TIME EXPIRED
}
