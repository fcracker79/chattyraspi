package io.mirko.alexa.home.raspberry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {
    public static byte[] getDataFromClassloader(String resourceName) {
        try(InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName);) {
            final byte[] buffer = new byte[32768];

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (int i = is.read(buffer); i >= 0; i = is.read(buffer)) {
                os.write(buffer, 0, i);
            }
            return os.toByteArray();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Properties getPropertiesFromClassloader(String resourceName) {
        final Properties prop = new Properties();
        try(InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {
            prop.load(is);
            return prop;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
