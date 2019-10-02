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


public class Test05FilterSolicitationByCompany {

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
        Map<String, Object> userCTEEP = DataUtil.buildPayloadUser("testercteep05", "tester123", "CTEEP");
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
        Map<String, Object> userONS = DataUtil.buildPayloadUser("testerons05", "tester123", "ONS");
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
    public void test01FilterWithValidCompanyCodeByUserCteep() {
        //Arrange
        String cteepAccessToken = ServiceUtil.doLogin("testercteep05", "tester123");
        String onsAccessToken = ServiceUtil.doLogin("testerons05", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "LIGAR", "REATOR", "MIR", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(1);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "PIR", "2", "", true, "CTEEP");
        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEP").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("action_code", hasItems("LIGAR", "DESLIGAR")).
                    body("equipment_code", hasItems("REATOR", "CAPACITOR")).
                    body("substation_code", hasItems("MIR", "PIR")).
                    body("amount", hasItems("5", "2")).
                    body("request_user_id", hasItems(userIDONS)).
                    body("status", hasItems("NOT_ANSWERED"));

        ServiceUtil.wait(2);

    }

    @Test
    public void test02FilterWithInvalidCompanyCodeByUserCteep() {
        //Arrange
        String cteepAccessToken = ServiceUtil.doLogin("testercteep05", "tester123");
        String onsAccessToken = ServiceUtil.doLogin("testerons05", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "LIGAR", "REATOR", "MIR", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(1);
        ServiceUtil.createSingleSolicitation(onsAccessToken, "DESLIGAR", "CAPACITOR", "PIR", "5", "", true, "CTEEP");
        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CTEEPO").
                    get("voltage_control_solicitation").
                then().
                    statusCode(404).
                    body("error", equalTo("Company not found"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    param("company_code", "CHESF").
                    get("voltage_control_solicitation").
                then().
                    statusCode(403).
                    body("error", equalTo("User can only access the solicitations of your company"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + cteepAccessToken).
                when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(400).
                    body("error", equalTo("Company code not informed"));

        ServiceUtil.wait(2);

    }

    @Test
    public void test03FilterWithValidCompanyCodeByUserONS() {

        //Arrange
        String onsAccessToken = ServiceUtil.doLogin("testerons05", "tester123");

        ServiceUtil.createSingleSolicitation(onsAccessToken, "ELEVAR", "SINCRONO", "MIR", "5", "500kV", true, "CTEEP");
        ServiceUtil.wait(1);

        ServiceUtil.createSingleSolicitation(onsAccessToken, "REDUZIR", "TAP", "PIR", "2", "140kV", true, "CTEEP");
        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken).
                when().
                    param("company_code", "CTEEP").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("action_code", hasItems("ELEVAR", "REDUZIR")).
                    body("equipment_code", hasItems("SINCRONO", "TAP")).
                    body("substation_code", hasItems("MIR", "PIR")).
                    body("amount", hasItems("5", "2")).
                    body("request_user_id", hasItems(userIDONS)).
                    body("status", hasItems("NOT_ANSWERED")).
                    body("voltage", hasItems("500kV" , "140kV"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken).
                when().
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("action_code", hasItems("ELEVAR", "REDUZIR")).
                    body("equipment_code", hasItems("SINCRONO", "TAP")).
                    body("substation_code", hasItems("MIR", "PIR")).
                    body("amount", hasItems("5", "2")).
                    body("request_user_id", hasItems(userIDONS)).
                    body("status", hasItems("NOT_ANSWERED")).
                    body("voltage", hasItems("500kV" , "140kV"));

        ServiceUtil.wait(2);

        //Act & Assert
        RestAssured.
                given().
                    header("Authorization", "Bearer " + onsAccessToken).
                when().
                    param("company_code", "CHESF").
                    get("voltage_control_solicitation").
                then().
                    statusCode(200).
                    body("$", equalTo(Collections.emptyList()));

        ServiceUtil.wait(2);

    }


}
