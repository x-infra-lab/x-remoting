package io.github.xinfra.lab.remoting.config;

import org.apache.commons.lang3.StringUtils;

/**
 * default value  < system properties
 */
public class ConfigManager {
    public static <T> T get(ConfigKey<T> configKey) {
        String propertyValue = System.getProperty(configKey.getConfigKey());
        if (StringUtils.isNoneBlank(propertyValue)) {
            return (T) convert(propertyValue, configKey.getConfigDefaultValue().getClass());
        }
        return configKey.getConfigDefaultValue();
    }

    private static Object convert(String propertyValue, Class<?> clazz) {
        if (Boolean.class.isAssignableFrom(clazz)) {
            return Boolean.parseBoolean(propertyValue);
        } else if (String.class.isAssignableFrom(clazz)) {
            return propertyValue;
        } else if (Short.class.isAssignableFrom(clazz)) {
            return Short.parseShort(propertyValue);
        } else if (Integer.class.isAssignableFrom(clazz)) {
            return Integer.parseInt(propertyValue);
        } else if (Long.class.isAssignableFrom(clazz)) {
            return Long.parseLong(propertyValue);
        } else if (Float.class.isAssignableFrom(clazz)) {
            return Float.parseFloat(propertyValue);
        } else if (Double.class.isAssignableFrom(clazz)) {
            return Double.parseDouble(propertyValue);
        } else if (Byte.class.isAssignableFrom(clazz)){
            return Byte.parseByte(propertyValue);
        }

        return propertyValue;
    }
}
