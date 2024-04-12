package com.example.resolver;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

@Component
public class IntegerCodeToEnumConverterFactory<T extends Enum> implements ConverterFactory<Integer, T> {

    @Override
    public <T1 extends T> Converter<Integer, T1> getConverter(Class<T1> targetType) {
        return new IntegerCodeToEnumConverter<>(targetType);
    }

    private static class IntegerCodeToEnumConverter<T extends Enum<T>> implements Converter<Integer, T> {
        private final Class<T> enumType;

        public IntegerCodeToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(Integer source) {
            if (source == null) {
                throw new IllegalArgumentException("Source integer cannot be null");
            }
            for (T enumConstant : enumType.getEnumConstants()) {
                if (enumConstant.ordinal() == source) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException("No element in " + enumType.getCanonicalName() + " with code " + source);
        }
    }

}