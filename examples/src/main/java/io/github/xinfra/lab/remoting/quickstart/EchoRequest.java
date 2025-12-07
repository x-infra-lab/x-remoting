package io.github.xinfra.lab.remoting.quickstart;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EchoRequest implements Serializable {
    String message;
}