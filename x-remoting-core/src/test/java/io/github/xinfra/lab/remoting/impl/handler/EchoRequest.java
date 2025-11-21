package io.github.xinfra.lab.remoting.impl.handler;

import java.io.Serializable;

public class EchoRequest implements Serializable {

	private String msg;

	public EchoRequest() {
	}

	public EchoRequest(String msg) {
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
