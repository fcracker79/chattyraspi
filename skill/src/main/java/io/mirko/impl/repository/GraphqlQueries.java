package io.mirko.impl.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class GraphqlQueries {
    private static final Map<String, String> QUERIES = new HashMap<>();

    private GraphqlQueries() {}

    private static String query(String name) {
        String query = QUERIES.get(name);
        if (query == null) {
            try (InputStream is = GraphqlQueries.class.getClassLoader().getResourceAsStream(name);) {
                final byte[] buffer = new byte[32768];

                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                for (int i = is.read(buffer); i >= 0; i = is.read(buffer)) {
                    os.write(buffer, 0, i);
                }
                query = new String(os.toByteArray()).intern();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            QUERIES.put(name, query);
        }
        return query;
    }

    public static String fetchCommandStatus() {
        return query("io/mirko/graphql/fetch_command_status.graphql");
    }

    public static String fetchCommandResponse() {
        return query("io/mirko/graphql/fetch_command_response.graphql");
    }

    public static String submitCommand() {
        return query("io/mirko/graphql/submit_command.graphql");
    }

    public static String deleteCommand() {
        return query("io/mirko/graphql/delete_command.graphql");
    }
}
