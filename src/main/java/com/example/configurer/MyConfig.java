package com.example.configurer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: badBoy
 * @create: 2025-07-02 16:01
 * @Description:
 */
@Configuration
public class MyConfig {

    private Map<String, String> map = new HashMap<>();

    public MyConfig(@Value("${my.map:#{null}}") String mapString) {
        // 手动解析字符串
        if (StringUtils.isNotBlank(mapString)) {
            this.map = Arrays.stream(mapString.split(","))
                    .map(entry -> entry.split(":"))
                    .filter(pair -> pair.length == 2)
                    .collect(Collectors.toMap(pair -> pair[0].trim(), pair -> pair[1].trim()));
        }
    }

    public Map<String, String> getMap() {
        return map;
    }

}
