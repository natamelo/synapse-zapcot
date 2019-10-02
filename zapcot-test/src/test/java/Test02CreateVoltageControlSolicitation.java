import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;

import util.DataUtil;
import util.ServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test02CreateVoltageControlSolicitation {

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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons02", "tester123", "ONS");
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


        // Create a CTEEP user
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep02", "tester123", "CTEEP");
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

        ServiceUtil.wait(5);

    }

    @Test
    public void test01CreateValidSolicitations() {

        String access_token = ServiceUtil.doLogin("testerons02", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
        "5", "", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));

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
        
        payloadSolicitation.put("action", "ELEVAR");
        payloadSolicitation.put("equipment", "SINCRONO");
        payloadSolicitation.put("substation", "SAL");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));
                    
        payloadSolicitation.put("action", "REDUZIR");
        payloadSolicitation.put("equipment", "TRANSFORMADOR");
        payloadSolicitation.put("substation", "PIR");

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

    @Test
    public void test02createSingleSolicitationsWithInvalidEquipment() {
        
        String access_token = ServiceUtil.doLogin("testerons02", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "INVALIDEQUIPMENT", "MOS",
        "5", "500kV", true, "CTEEP");
        
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid Equipment!"));
        
        ServiceUtil.wait(5);
    }


    @Test
    public void test03createSingleSolicitationsWithInvalidAction() {
        String access_token = ServiceUtil.doLogin("testerons02", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("INVALID ACTION", "CAPACITOR", "MOS",
        "5", "", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid action!"));

        ServiceUtil.wait(5);
    }


    @Test
    public void test04createSingleSolicitationsWithInvalidSubstation() {
        String access_token = ServiceUtil.doLogin("testerons02", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "INVALID SUBSTATION",
        "5", "", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Invalid substation!"));
        
        ServiceUtil.wait(5);
    }

    @Test
    public void test05createSingleSolicitationsWithUnauthorizedUser() {

        String access_token = ServiceUtil.doLogin("testercteep02", "tester123");

        ServiceUtil.wait(2);

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation("LIGAR", "CAPACITOR", "MOS",
        "5", "", true, "CTEEP");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(payloadSolicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(401).
                    body("soft_logout", equalTo("User should to belong ONS."));

        ServiceUtil.wait(5);
    }

    public void test06CreateMultipleSolicitations() {

        String access_token = ServiceUtil.doLogin("testerons02", "tester123");
        
        Map<String, Object> solicitation1 = DataUtil.buildSingleSolicitation("LIGAR", "REATOR", "MOS", 
        "5", "50", true, "CTEEP");
        Map<String, Object> solicitation2 = DataUtil.buildSingleSolicitation("DESLIGAR", "CAPACITOR", "ATI", 
        "5", "", true, "CTEEP");
        Map<String, Object> solicitation3 = DataUtil.buildSingleSolicitation("LIGAR", "CAPACITOR", "ATI", 
        "5", "", true, "CTEEP");

        List<Map<String, Object>> solicitations = new ArrayList<>();
        solicitations.add(solicitation1);
        solicitations.add(solicitation2);
        solicitations.add(solicitation3);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitations).
                when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body("message", equalTo("Voltage control solicitations created with success."));
    }

}
