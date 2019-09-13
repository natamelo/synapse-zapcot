import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;


public class Test04FilterTable {

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
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep04", "tester123", "CTEEP");
        String session = ServiceUtil.getSession(userCTEEP);
        userCTEEP.put("auth", ServiceUtil.getAuthObject(session));

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
    public void test01FilterWithValidCompanyCode() {
        //Arrange
        Map<String, Object> payloadLogin = DataUtil.
                buildPayloadLogin("testercteep04", "tester123");

        String access_token = RestAssured.
                given().
                    body(payloadLogin).
                when().
                    post("login").
                then().
                    statusCode(200).
                    extract().path("access_token");

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                when().
                    param("company_code", "CTEEP").
                    get("tables").
                then().
                    statusCode(200).
                    body("code", hasItems("A1", "A2"));

    }

    @Test
    public void test02FilterWithInvalidCompanyCode() {

        //Arrange
        Map<String, Object> payloadLogin = DataUtil.
                buildPayloadLogin("testercteep04", "tester123");

        String access_token = RestAssured.
                given().
                    body(payloadLogin).
                when().
                    post("login").
                then().
                    statusCode(200).
                    extract().path("access_token");

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                when().
                    get("tables").
                then().
                    statusCode(400).
                    body("error", equalTo("It's necessary the param company code"));

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                when().
                    param("company_code", "NO_EXIST").
                    get("tables").
                then().
                    statusCode(404).
                    body("error", equalTo("Company not found"));

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + access_token).
                when().
                    param("company_code", "CHESF").
                    get("tables").
                then().
                    statusCode(403).
                    body("error", equalTo("User can only access the solicitations of your company"));
        
    }

}
