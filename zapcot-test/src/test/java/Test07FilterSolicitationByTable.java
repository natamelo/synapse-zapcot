import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.BeforeClass;
import org.junit.Test;
import util.DataUtil;
import util.ServiceUtil;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;


public class Test07FilterSolicitationByTable {

    private static String userIDONS;

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
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep07", "tester123", "CTEEP");
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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons07", "tester123", "ONS");
        session = ServiceUtil.getSession(userONS);
        userONS.put("auth", ServiceUtil.getAuthObject(session));

        userIDONS = RestAssured.
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
    public void test01FilterWithValidTableCodeByUserCteep() {
        //Arrange
        String cteepAccessToken = ServiceUtil.doLogin("testercteep07", "tester123");
        String onsAccessToken = ServiceUtil.doLogin("testerons07", "tester123");

        ServiceUtil.createSolicitation(onsAccessToken, "LIGAR", "REATOR", "MIR", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(1);
        ServiceUtil.createSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "PIR", "10", "140kV", true, "CTEEP");
        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "A1").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("action_code", hasItems("LIGAR", "DESLIGAR")).
                    body("equipment_code", hasItems("REATOR", "CAPACITOR")).
                    body("substation_code", hasItems("MIR", "PIR")).
                    body("amount", hasItems("5", "10")).
                    body("request_user_id", hasItems(userIDONS)).
                    body("status", hasItems("NOT_ANSWERED")).
                    body("voltage", hasItems("500kV" , "140kV"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "A3").
                get("voltage_control_solicitation").
                    then().
                    statusCode(200).
                    body("$", equalTo(Collections.emptyList()));;

        ServiceUtil.wait(2);

        ServiceUtil.createSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "SAL", "5", "500kV", true, "CTEEP");

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "A3").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("substation_code", hasItems("SAL"));

        ServiceUtil.wait(2);

        ServiceUtil.createSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "SAL", "5", "", true, "CTEEP");

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "A3").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("substation_code", hasItems("SAL"));

        ServiceUtil.wait(2);
    }

    @Test
    public void test02FilterWithInvalidTableCodeByUserCteep() {
        //Arrange
        String cteepAccessToken = ServiceUtil.doLogin("testercteep07", "tester123");

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "TORADA").
                    get("voltage_control_solicitation").
                then().
                    statusCode(404).
                    body("error", equalTo("Table not found"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    param("table_code", "A4").
                    get("voltage_control_solicitation").
                then().
                    statusCode(404).
                    body("error", equalTo("Table not found"));

        ServiceUtil.wait(2);

    }

}
