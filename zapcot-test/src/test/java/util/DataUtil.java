package util;

import java.util.*;

public class DataUtil {

    public static Map<String, Object> buildPayloadUser(String userName, String password,
                                                       String companyCode) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", userName);
        user.put("password", password);
        user.put("bind_email", true);
        user.put("bind_msisdn", true);
        user.put("x_show_msisdn", true);
        user.put("company_code", companyCode);
        return user;
    }

    public static Map<String, Object> buildPayloadLogin(String username, String password) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "m.login.password");
        payload.put("password", password);
        payload.put("initial_device_display_name", "JUnit");
        payload.put(username, true);

        Map<String, Object> identifier = new HashMap<>();
        identifier.put("type", "m.id.user");
        identifier.put("user", username);

        payload.put("identifier", identifier);

        return payload;
    }


    public static Map<String, Object> buildPayloadSolicitation(String action, String equipment, String substation,
                                                               String amount, String voltage, Boolean chaining, String company_code) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("action", action);
        payload.put("equipment", equipment);
        payload.put("substation", substation);
        payload.put("amount", amount);
        payload.put("voltage", voltage);
        payload.put("chaining", chaining);
        payload.put("company_code", company_code);

        return payload;
    }

    public static Map<String, Object> buildPayloadTables(String... tables) {
        Map<String, Object> payloadTables = new HashMap<>();
        payloadTables.put("tables", Arrays.asList(tables));
        return payloadTables;
    }

    public static Map<String, String> buildPayloadChangeStatus(String status) {
        Map<String, String> payload = new HashMap<>();
        payload.put("status", status);
        return payload;
    }
}
