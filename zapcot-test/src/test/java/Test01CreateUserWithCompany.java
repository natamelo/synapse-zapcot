import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Map;


public class Test01CreateUserWithCompany {

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
    public void test01CreateValidUsers() {
        //Arrange
        Map<String, Object> userTesterONS = DataUtil.buildPayloadUser("testerons", "tester123", "ONS");
        String session = ServiceUtil.getSession(userTesterONS);
        userTesterONS.put("auth", ServiceUtil.getAuthObject(session));

        //Act
        String userId = RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(userTesterONS).
                when().
                    post("register").
                then().
                    statusCode(200).
                    extract().path("user_id");

        //Assert
        Assert.assertThat(userId, CoreMatchers.startsWith("@testerons"));

        ServiceUtil.wait(5);

        //Arrange
        Map<String, Object> userTesterCTEEP = DataUtil.buildPayloadUser("testercteep", "tester123", "CTEEP");
        session = ServiceUtil.getSession(userTesterCTEEP);
        userTesterCTEEP.put("auth", ServiceUtil.getAuthObject(session));

        //Act
        userId = RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(userTesterCTEEP).
                when().
                    post("register").
                then().
                    statusCode(200).
                    extract().path("user_id");

        //Assert
        Assert.assertThat(userId, CoreMatchers.startsWith("@testercteep"));

        ServiceUtil.wait(5);

        //Arrange
        Map<String, Object> userTesterCHESF = DataUtil.buildPayloadUser("testerchesf", "tester123", "CHESF");
        session = ServiceUtil.getSession(userTesterCTEEP);
        userTesterCHESF.put("auth", ServiceUtil.getAuthObject(session));

        //Act
        userId = RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(userTesterCHESF).
                when().
                    post("register").
                then().
                    statusCode(200).
                    extract().path("user_id");

        //Assert
        Assert.assertThat(userId, CoreMatchers.startsWith("@testerchesf"));

        ServiceUtil.wait(5);

    }

    @Test
    public void test02CreateUserWithInvalidCompany() {

        //Arrange
        Map<String, Object> user = DataUtil.buildPayloadUser("tester", "tester123", "TORADA");
        String session = ServiceUtil.getSession(user);
        user.put("auth", ServiceUtil.getAuthObject(session));

        //Act & Assert
        RestAssured.
                given().
                    contentType(ContentType.JSON).
                    body(user).
                when().
                    post("register").
                then().
                    statusCode(400);

        ServiceUtil.wait(5);

    }

}
