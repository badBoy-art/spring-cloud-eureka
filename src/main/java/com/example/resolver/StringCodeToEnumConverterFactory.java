package com.example.resolver;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author: badBoy
 * @create: 2024-04-12 11:13
 * @Description:
 */
@Component
public class StringCodeToEnumConverterFactory<T extends Enum> implements ConverterFactory<String, T> {

    @Override
    public <T1 extends T> Converter<String, T1> getConverter(Class<T1> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private static class StringToEnumConverter<T extends Enum> implements Converter<String, T> {
        private final Class<T> enumType;

        public StringToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (StringUtils.isBlank(source)) {
                throw new IllegalArgumentException("Source integer cannot be null");
            }
            for (T enumConstant : enumType.getEnumConstants()) {
                if (Objects.equals(enumConstant.name(), source)) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException("No element in " + enumType.getCanonicalName() + " with code " + source);
        }
    }
}
