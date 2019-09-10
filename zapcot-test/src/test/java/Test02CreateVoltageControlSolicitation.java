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
        login.put("testerons1", true);

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


//        Map<String, Object> auth = new HashMap<>();
//        auth.put("session", session);
//        auth.put("type", "m.login.dummy");
//
//        user.put("auth", auth);
//
//        String userId = RestAssured.given().body(user).
//                when().post("register").then().statusCode(200).
//                extract().path("user_id");
//
//        Assert.assertThat(userId, CoreMatchers.startsWith("@testerONS"));
//
//        user.put("username", "testerCTEEP");
//        user.put("company_code", "CTEEP");
//
//        userId = RestAssured.given().body(user).
//                when().post("register").then().statusCode(200).
//                extract().path("user_id");
//
//        Assert.assertThat(userId, CoreMatchers.startsWith("@testerCTEEP"));
//
//        user.put("username", "testerCHESF");
//        user.put("company_code", "CHESF");
//
//        userId = RestAssured.given().body(user).
//                when().post("register").then().statusCode(200).
//                extract().path("user_id");
//
//        Assert.assertThat(userId, CoreMatchers.startsWith("@testerCHESF"));

        ServiceUtil.wait(5);
    }

}
