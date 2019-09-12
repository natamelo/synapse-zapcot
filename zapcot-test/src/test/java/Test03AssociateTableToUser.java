import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;


public class Test03AssociateTableToUser {

    private static String userID;

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

        // Create an user
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep03", "tester123", "CTEEP");
        String session = ServiceUtil.getSession(userCTEEP);
        userCTEEP.put("auth", ServiceUtil.getAuthObject(session));
        userCTEEP.put("admin", true);

        userID = RestAssured.
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
    public void test01AssociateValidTableToUsers() {
        //Arrange
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testercteep03", "tester123");

        String access_token = RestAssured.
                given().
                body(payloadLogin).
                when().
                post("login").
                then().
                statusCode(200).
                extract().path("access_token");

        Map<String, Object> payloadTables = DataUtil.buildPayloadTables("A1");

        //Act & Assert
        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadTables)
                .when().
                put("associate_tables/" + userID).
                then().
                statusCode(200).
                body(equalTo("\"The tables were associated with the user!\""));

    }

    @Test
    public void test02AssociateInvalidTableToUsers() {

        //Arrange
        Map<String, Object> payloadLogin = DataUtil.buildPayloadLogin("testercteep03", "tester123");

        String access_token = RestAssured.
                given().
                body(payloadLogin).
                when().
                post("login").
                then().
                statusCode(200).
                extract().path("access_token");


        Map<String, Object> payloadTables = DataUtil.buildPayloadTables("XX");

        //Act & Assert
        RestAssured.
                given().
                header("Authorization", "Bearer " + access_token).
                body(payloadTables)
                .when().
                put("associate_tables/" + userID).
                then().
                statusCode(400).
                body("error", equalTo("One or more invalid table!"));

    }

}
