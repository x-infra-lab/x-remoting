package io.github.xinfra.lab.remoting.config;

public enum ConfigKeys {

    idle_switch("idle_switch", "true"),
    idle_reader_timeout("idle_reader_timeout", "15000"),
    idle_writer_timeout("idle_writer_timeout", "15000"),
    idle_all_timeout("idle_all_timeout", "15000"),

    ;

    private String configKey;

    private String configDefaultValue;

    ConfigKeys(String configKey, String configDefaultValue) {
        this.configKey = configKey;
        this.configDefaultValue = configDefaultValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigDefaultValue() {
        return configDefaultValue;
    }
}
