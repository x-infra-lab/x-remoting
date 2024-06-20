package io.github.xinfra.lab.remoting.config;

import lombok.Getter;
import org.apache.commons.lang3.Validate;

@Getter
public class ConfigKey<T> {

    public static final ConfigKey<Boolean> IDLE_SWITCH = new ConfigKey<>("idle_switch", true);
    public static final ConfigKey<Long> IDLE_READER_TIMEOUT = new ConfigKey<>("idle_reader_timeout", 15000L);
    public static final ConfigKey<Long> IDLE_WRITER_TIMEOUT = new ConfigKey<>("idle_writer_timeout", 15000L);
    public static final ConfigKey<Long> IDLE_ALL_TIMEOUT = new ConfigKey<>("idle_all_timeout", 15000L);


    private final String configKey;

    private final T configDefaultValue;

    public ConfigKey(String configKey, T configDefaultValue) {
        Validate.notNull(configKey, "configKey can not be null.");
        Validate.notNull(configDefaultValue, "configDefaultValue can not be null.");
        this.configKey = configKey;
        this.configDefaultValue = configDefaultValue;
    }
}
