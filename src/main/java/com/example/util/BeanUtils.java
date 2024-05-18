package com.example.util;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;

/**
 * @author: badBoy
 * @create: 2024-05-17 23:40
 * @Description:
 */
@Component
public class BeanUtils {

    private static String defaultJavaCharset = "8859_1";

    private BeanUtils() {
    }

    @Value("${file.encoding}")
    public void setDefaultJavaCharset(String defaultJavaCharset) {
        this.defaultJavaCharset = defaultJavaCharset;
    }

    public static String getDefaultJavaCharset() {
        if (StringUtils.isBlank(defaultJavaCharset)) {
            //String mimecs = null;
            //
            //try {
            //    mimecs = System.getProperty("mail.mime.charset");
            //} catch (SecurityException var3) {
            //}
            //
            //if (mimecs != null && mimecs.length() > 0) {
            //    defaultJavaCharset = javaCharset(mimecs);
            //    return defaultJavaCharset;
            //}
            InputStreamReader reader = new InputStreamReader(new NullInputStream());
            defaultJavaCharset = reader.getEncoding();
            if (StringUtils.isBlank(defaultJavaCharset)) {
                defaultJavaCharset = "8859_1";
            }
        }

        return defaultJavaCharset;
    }
}
