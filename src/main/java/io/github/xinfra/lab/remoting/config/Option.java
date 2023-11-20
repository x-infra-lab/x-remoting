package io.github.xinfra.lab.remoting.config;

import lombok.Getter;

@Getter
public class Option<V> {
    private String name;

    private V defaultValue;

    public Option(String name, V defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }
}
