package com.example.util;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Locale;

/**
 * @author: badBoy
 * @create: 2024-05-17 23:40
 * @Description:
 */
@Component
public class BeanUtils {

    private static String defaultJavaCharset = "8859_1";
    private static String defaultMIMECharset;
    private static Hashtable mime2java = new Hashtable(10);
    private static Hashtable java2mime = new Hashtable(40);

    private BeanUtils() {
    }

    public static String getDefaultMIMECharset() {
        if (StringUtils.isBlank(defaultMIMECharset)) {
            defaultMIMECharset = mimeCharset(getDefaultJavaCharset());
        }

        return defaultMIMECharset;
    }

    public static String mimeCharset(String charset) {
        if (java2mime != null && charset != null) {
            String alias = (String) java2mime.get(charset.toLowerCase(Locale.ENGLISH));
            return alias == null ? charset : alias;
        } else {
            return charset;
        }
    }

    @Value("${file.encoding:#{null}}")
    public void setDefaultJavaCharset(String defaultJavaCharset) {
        this.defaultJavaCharset = defaultJavaCharset;
    }

    @Value("${mail.mime.charset:#{null}}")
    public void setDefaultMIMECharset(String defaultMIMECharset) {
        this.defaultMIMECharset = defaultMIMECharset;
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
