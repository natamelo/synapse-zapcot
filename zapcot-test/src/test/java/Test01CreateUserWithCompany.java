import com.jayway.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
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

        Map<String, Object> user = new HashMap<>();
        //user.put("auth", null);
        user.put("password", "tester123");
        user.put("bind_email", true);
        user.put("bind_msisdn", true);
        user.put("x_show_msisdn", true);
        user.put("company_code", "ONS");

        String session = RestAssured.given().body(user).
                when().post("register").then().extract().path("session");

        Map<String, Object> auth = new HashMap<>();
        auth.put("session", session);
        auth.put("type", "m.login.dummy");

        user.put("auth", auth);

        user.put("username", "testerons");
        user.put("company_code", "ONS");

        String userId = RestAssured.given().body(user).
                when().post("register").then().statusCode(200).
                extract().path("user_id");

        Assert.assertThat(userId, CoreMatchers.startsWith("@testerons"));

        session = RestAssured.given().body(user).
                when().post("register").then().extract().path("session");

        auth.put("session", session);

        user.put("username", "testercteep");
        user.put("company_code", "CTEEP");
        user.put("auth", auth);

        userId = RestAssured.given().body(user).
                when().post("register").then().statusCode(200).
                extract().path("user_id");

        Assert.assertThat(userId, CoreMatchers.startsWith("@testercteep"));

        session = RestAssured.given().body(user).
                when().post("register").then().extract().path("session");

        auth.put("session", session);

        user.put("username", "testerchesf");
        user.put("company_code", "CTEEP");
        user.put("auth", auth);

        userId = RestAssured.given().body(user).
                when().post("register").then().statusCode(200).
                extract().path("user_id");

        Assert.assertThat(userId, CoreMatchers.startsWith("@testerchesf"));

    }

    @Test
    public void test02CreateUserWithInvalidCompany() {

        Map<String, Object> user = new HashMap<>();
        user.put("username", "tester");
        user.put("password", "tester123");
        user.put("bind_email", true);
        user.put("bind_msisdn", true);
        user.put("x_show_msisdn", true);
        user.put("company_code", "TORADA");

        String session = RestAssured.given().body(user).
                when().post("register").then().extract().path("session");

        Map<String, Object> auth = new HashMap<>();
        auth.put("session", session);
        auth.put("type", "m.login.dummy");

        user.put("auth", auth);

        RestAssured.given().body(user).
                when().post("register").then().statusCode(400);

    }


}
