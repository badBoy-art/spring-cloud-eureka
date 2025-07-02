package com.example.resolver;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: badBoy
 * @create: 2025-07-02 16:09
 * @Description:
 */
@Component
public class StringToMapConverter implements Converter<String, Map<String, String>> {
    @Override
    public Map<String, String> convert(String source) {
        Map<String, String> map = new HashMap<>();
        if (source != null && !source.isEmpty()) {
            Arrays.stream(source.split(","))
                    .map(entry -> entry.split(":"))
                    .forEach(pair -> {
                        if (pair.length == 2) {
                            map.put(pair[0].trim(), pair[1].trim());
                        }
                    });
        }
        return map;
    }
}