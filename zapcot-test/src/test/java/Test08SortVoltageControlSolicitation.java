import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test08SortVoltageControlSolicitation {

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
    public void test01SortByCreationTime() {

        String onsAccessToken = ServiceUtil.doLogin("testerons", "tester123");

        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "TES", "1", "5");
        ServiceUtil.wait(5);
        createSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "TES", "2", "10");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=creation_time").
                then().
                    statusCode(200).
                    body("get(0).action_code", equalTo("DESLIGAR"));

        ServiceUtil.wait(5);

        createSolicitation(onsAccessToken, "LIGAR", "CAPACITOR", "TES", "2", "10");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=creation_time").
                then().
                    statusCode(200).
                    body("get(0).action_code", equalTo("LIGAR"));

    }


    @Test
    public void test02SortBySubstations() {

        String onsAccessToken = ServiceUtil.doLogin("testerons", "tester123");

        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);
        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=creation_time").
                then().
                    statusCode(200).
                    body("get(1).substation_code", equalTo("ATI"));

    }

    @Test
    public void test03SortByStatus() {

        String onsAccessToken = ServiceUtil.doLogin("testerons", "tester123");

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=status").
                then().
                    statusCode(200).
                    body("get(0).status", equalTo("NOT_ANSWERED"));

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=status").
                then().
                    statusCode(200).
                    body("get(2).status", equalTo("NOT_ANSWERED"));

    }

    @Test
    public void test04sortByStatusAndSubstation() {

        String onsAccessToken = ServiceUtil.doLogin("testerons", "tester123");

        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);
        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=status+substation").
                then().
                    statusCode(200).
                    body("get(0).substation_code", equalTo("ATI")).
                    body("get(0).status", equalTo("NOT_ANSWERED")).
                    body("get(1).substation_code", equalTo("ATI")).
                    body("get(1).status", equalTo("NOT_ANSWERED"));

    }

    @Test
    public void test05sortByStatusAndSubstationAndCreationTime() {

        String onsAccessToken = ServiceUtil.doLogin("testerons", "tester123");

        createSolicitation(onsAccessToken, "LIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);
        createSolicitation(onsAccessToken, "DESLIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);
        createSolicitation(onsAccessToken, "DESLIGAR", "REATOR", "ATI", "1", "5");
        ServiceUtil.wait(5);


        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=status+substation").
                then().
                    statusCode(200).
                    body("get(0).substation_code", equalTo("ATI")).
                    body("get(0).action_code", equalTo("DESLIGAR")).
                    body("get(0).status", equalTo("NOT_ANSWERED")).
                    body("get(1).substation_code", equalTo("ATI")).
                    body("get(1).action_code", equalTo("DESLIGAR")).
                    body("get(1).status", equalTo("NOT_ANSWERED")).
                    body("get(2).substation_code", equalTo("ATI")).
                    body("get(2).action_code", equalTo("LIGAR")).
                    body("get(2).status", equalTo("NOT_ANSWERED"));

    }

    private void createSolicitation (String onsAccessToken, String action,
                                     String equipment, String substation,
                                     String bar, String value) {

        Map<String, String> payloadSolicitation = DataUtil.buildPayloadSolicitation(
                action, equipment, substation, bar, value);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken).
                    body(payloadSolicitation).
                when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201);

    }
}
