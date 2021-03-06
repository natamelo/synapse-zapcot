package util;

import io.restassured.RestAssured;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServiceUtil {

    public static String doLogin (String username, String password) {
        return RestAssured.
                given().
                    body(DataUtil.
                        buildPayloadLogin(username, password)).
                when().
                    post("login").
                then().
                    statusCode(200).
                    extract().path("access_token");
    }

    public static String getSession(Map<String, Object> user) {
        return RestAssured.given().body(user).
                when().post("register").then().extract().path("session");
    }

    public static Map<String, Object> getAuthObject(String session) {
        Map<String, Object> auth = new HashMap<>();
        auth.put("session", session);
        auth.put("type", "m.login.dummy");
        return auth;
    }

    public static void createSingleSolicitation (String onsAccessToken, String action,
                                     String equipment, String substation,
                                     String amount, String voltage, Boolean staggered, String company_code) {

        Map<String, Object> payloadSolicitation = DataUtil.buildPayloadSingleSolicitation(
                action, equipment, substation, amount, voltage, staggered, company_code);

        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken).
                    body(payloadSolicitation).
                when().
                    post("voltage_control_solicitation").
                then().
                    statusCode(201);

    }

    public static void wait (int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
