package util;

import java.util.HashMap;
import java.util.Map;

public class DataUtil {

    public static Map<String, Object> buildUser(String userName, String password,
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


}
