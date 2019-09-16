import io.restassured.RestAssured;

import org.junit.BeforeClass;
import org.junit.Test;

import util.DataUtil;
import util.ServiceUtil;

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

    }

    @Test
    public void test01CreateValidSolicitations() {

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
                    body(equalTo("\"Voltage control solicitation created with success.\""));
        
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
                    body(equalTo("\"Voltage control solicitation created with success.\""));
                    
        payloadSolicitation.put("action", "REDUZIR");
        payloadSolicitation.put("equipment", "TAP");
        payloadSolicitation.put("substation", "PIR");

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
    }

    @Test
    public void test02CreateSolicitationsWithInvalidEquipment() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "INVALIDEQUIPMENT", "MOS",
        "FASE", "5000kV");

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
    public void test03CreateSolicitationsWithInvalidAction() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("INVALID ACTION", "CAPACITOR", "MOS",
        "FASE", "5000kV");

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
    public void test04CreateSolicitationsWithInvalidSubstation() {
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testerons", "tester123");

        String access_token = RestAssured.
                    given().
                        body(payloadLogin).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation("LIGAR", "CAPACITOR", "INVALID SUBSTATION",
        "FASE", "5000kV");

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
    public void test03CreateSolicitationsWithUnauthorizedUser() {

        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testercteep", "tester123");;

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
                    statusCode(401).
                    body("soft_logout", equalTo("User should to belong ONS."));

        ServiceUtil.wait(5);
    }

}
