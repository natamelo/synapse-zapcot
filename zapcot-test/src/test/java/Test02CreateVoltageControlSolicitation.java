import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import util.ServiceUtil;

import java.util.HashMap;
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

        Map<String, Object> login = new HashMap<>();
        login.put("type", "m.login.password");
        login.put("password", "tester123");
        login.put("initial_device_display_name", "http://localhost:8080/ via Firefox em Ubuntu");
        login.put("testerons", true);

        Map<String, Object> identifier = new HashMap<>();
        identifier.put("type", "m.id.user");
        identifier.put("user", "testerons");

        login.put("identifier", identifier);

        String access_token = RestAssured.
                    given().
                        body(login).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> solicitation = new HashMap<>();

        solicitation.put("action", "LIGAR");
        solicitation.put("equipment", "CAPACITOR");
        solicitation.put("substation", "MOS");
        solicitation.put("bar", "FASE");
        solicitation.put("value", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));

        solicitation.put("action", "DESLIGAR");
        solicitation.put("equipment", "REATOR");
        solicitation.put("substation", "ATI");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));
        
        solicitation.put("action", "ELEVAR");
        solicitation.put("equipment", "SINCRONO");
        solicitation.put("substation", "SAL");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));
                    
        solicitation.put("action", "REDUZIR");
        solicitation.put("equipment", "TAP");
        solicitation.put("substation", "PIR");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201).
                    body(equalTo("\"Voltage control solicitation created with success.\""));
                    

        ServiceUtil.wait(5);
    }

    @Test
    public void test02CreateInvalidSolicitations() {
        Map<String, Object> login = new HashMap<>();
        login.put("type", "m.login.password");
        login.put("password", "tester123");
        login.put("initial_device_display_name", "http://localhost:8080/ via Firefox em Ubuntu");
        login.put("testerons", true);

        Map<String, Object> identifier = new HashMap<>();
        identifier.put("type", "m.id.user");
        identifier.put("user", "testerons");

        login.put("identifier", identifier);

        String access_token = RestAssured.
                    given().
                        body(login).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> solicitation = new HashMap<>();

        solicitation.put("action", "LIGAR");
        solicitation.put("equipment", "INVALIDEQUIPMENT");
        solicitation.put("substation", "MOS");
        solicitation.put("bar", "FASE");
        solicitation.put("value", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body(equalTo("\"Equipment type must be valid.\""));


        solicitation.put("action", "INVALID ACTION");
        solicitation.put("equipment", "CAPACITOR");
        solicitation.put("substation", "MOS");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body(equalTo("\"Action must be valid.\""));

        
        solicitation.put("action", "LIGAR");
        solicitation.put("equipment", "CAPACITOR");
        solicitation.put("substation", "INVALIDSUBSTATION");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body(equalTo("\"Substation code must be valid.\""));

    }

    @Test
    public void test03CreateSolicitationsWithUnauthorizedUser() {

        Map<String, Object> login = new HashMap<>();
        login.put("type", "m.login.password");
        login.put("password", "tester123");
        login.put("initial_device_display_name", "http://localhost:8080/ via Firefox em Ubuntu");
        login.put("testercteep", true);

        Map<String, Object> identifier = new HashMap<>();
        identifier.put("type", "m.id.user");
        identifier.put("user", "testercteep");

        login.put("identifier", identifier);

        String access_token = RestAssured.
                    given().
                        body(login).
                    when().
                        post("login").
                    then().
                        statusCode(200).
                        extract().path("access_token");

        Map<String, String> solicitation = new HashMap<>();

        solicitation.put("action", "LIGAR");
        solicitation.put("equipment", "CAPACITOR");
        solicitation.put("substation", "MOS");
        solicitation.put("bar", "FASE");
        solicitation.put("value", "5000kV");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                    body(solicitation)
                .when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(401).
                    body(equalTo("\"Permission denied.\""));


    }

}
