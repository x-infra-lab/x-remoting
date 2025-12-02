package io.github.xinfra.lab.remoting.impl.handler;

import io.github.xinfra.lab.remoting.common.Validate;

public class RequestApi {

	private String path;

	private RequestApi(String path) {
		Validate.notBlank(path, "path can not be blank");
		this.path = path;
	}

	public String path() {
		return path;
	}

	public static RequestApi of(String path) {
		return new RequestApi(path);
	}

}
