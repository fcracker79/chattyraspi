package io.mirko;

import org.json.JSONObject;

import java.util.Collections;

public class Main {
    public static void main(String ... args) throws Exception {
        JSONObject property = new JSONObject();
        property.put("test", new JSONObject((Object) Collections.singletonMap("aKey", "aValue")));
        property.put("test2", new JSONObject((Object) "hello"));
        System.out.println(property.toString());
    }
}
