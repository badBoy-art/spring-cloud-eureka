package com.example.util;

import com.example.service.StaticService;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
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
    private static final Logger logger = LoggerFactory.getLogger(BeanUtils.class);

    private static String defaultJavaCharset = "8859_1";
    private static String defaultMIMECharset;
    private static Hashtable mime2java = new Hashtable(10);
    private static Hashtable java2mime = new Hashtable(40);

    private static StaticService staticService;

    @Autowired
    public BeanUtils(StaticService staticService) {
        logger.info("construt staticService:{}", staticService.getStr());
        BeanUtils.staticService = staticService;
    }

    public static String getDefaultMIMECharset() {
        if (StringUtils.isBlank(defaultMIMECharset)) {
            defaultMIMECharset = mimeCharset(getDefaultJavaCharset());
        }

        return defaultMIMECharset + staticService.getStr();
    }

    static {
        logger.info("static staticService:{}", defaultJavaCharset);
    }

    {
        logger.info("code block staticService:{}", defaultJavaCharset);
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
        logger.info("setDefaultJavaCharset:{}", defaultJavaCharset);
        BeanUtils.defaultJavaCharset = defaultJavaCharset;
    }

    @Value("${mail.mime.charset:#{null}}")
    public void setDefaultMIMECharset(String defaultMIMECharset) {
        BeanUtils.defaultMIMECharset = defaultMIMECharset;
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

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        logger.info("applicationReady........");
    }

    @EventListener(ContextClosedEvent.class)
    public void contextClosed() {
        logger.error("contextClosed........");

    }


}
