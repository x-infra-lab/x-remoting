package io.github.xinfra.lab.remoting.impl.handler;

public class RequestApi {

	private String path;

	private RequestApi(String path) {
		this.path = path;
	}

	public String path() {
		return path;
	}

	public static RequestApi of(String path) {
		return new RequestApi(path);
	}

}
