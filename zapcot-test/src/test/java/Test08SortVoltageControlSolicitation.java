import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

    @Test
    public void test01SortByCreationTime() {

        String onsAccessToken = ServiceUtil.doLogin("testerons08", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "TES", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_OFF", "CAPACITOR", "TES", "5", "", true, "CTEEP");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=creation_time").
                then().
                    statusCode(200).
                    body("get(0).action_code", equalTo("TURN_OFF"));

        ServiceUtil.wait(5);

        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "CAPACITOR", "TES", "5", "", true, "CTEEP");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=creation_time").
                then().
                    statusCode(200).
                    body("get(0).action_code", equalTo("TURN_ON"));

        ServiceUtil.wait(2);

    }


    @Test
    public void test02SortBySubstations() {

        String onsAccessToken = ServiceUtil.doLogin("testerons08", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=substation").
                then().
                    statusCode(200).
                    body("get(1).substation_code", equalTo("ATI"));

    }

    @Test
    public void test03SortByStatus() {

        String onsAccessToken = ServiceUtil.doLogin("testerons08", "tester123");

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

        String onsAccessToken = ServiceUtil.doLogin("testerons08", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
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

        String onsAccessToken = ServiceUtil.doLogin("testerons08", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_ON", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_OFF", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "TURN_OFF", "REACTOR", "ATI", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(5);


        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken)
                .when().
                    get("voltage_control_solicitation?sort=status+creation_time+substation").
                then().
                    statusCode(200).
                    body("get(0).substation_code", equalTo("ATI")).
                    body("get(0).action_code", equalTo("TURN_OFF")).
                    body("get(0).status", equalTo("NOT_ANSWERED")).
                    body("get(1).substation_code", equalTo("ATI")).
                    body("get(1).action_code", equalTo("TURN_OFF")).
                    body("get(1).status", equalTo("NOT_ANSWERED")).
                    body("get(2).substation_code", equalTo("ATI")).
                    body("get(2).action_code", equalTo("TURN_ON")).
                    body("get(2).status", equalTo("NOT_ANSWERED"));

    }

}
