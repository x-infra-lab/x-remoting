package io.github.xinfra.lab.remoting.rpc.client;

import java.io.Serializable;

public class SimpleRequest implements Serializable {
    private String msg;

    public SimpleRequest() {
    }

    public SimpleRequest(String msg) {
        this.msg = msg;
    }


    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
