package io.github.xinfra.lab.remoting.config;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private Map<Option<?>, Object> map = new HashMap<>();


    public <T> void option(Option<T> option, T value) {
        map.put(option, value);
    }

    public <T> T option(Option<T> option) {
        Object value = map.get(option);
        if (value == null) {
            return option.getDefaultValue();
        } else {
            return (T) value;
        }
    }

}
