import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.OrderUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test08OrderByONSSolicitation {

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

        OrderUtil.createSolicitationWithStatusNew();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW"));

        OrderUtil.createSolicitationWithStatusCanceled();

        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token)
                .when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("get(0).events[0].status", equalTo("NEW")).
                    body("get(1).events[0].status", equalTo("CANCELED"));

        OrderUtil.createSolicitationWithStatusAccepted();

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

        OrderUtil.createSolicitationWithStatusExecuted();

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

        OrderUtil.createSolicitationWithStatusBlocked();

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

        OrderUtil.createSolicitationWithStatusRequired();

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

        OrderUtil.createSolicitationWithStatusContested();

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

}
