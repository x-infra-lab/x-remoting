package io.github.xinfra.lab.remoting.impl.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RequestHandlerRegistry {

	private ConcurrentHashMap<RequestApi, RequestHandler> registry = new ConcurrentHashMap<>();

	public <T, R> void register(RequestApi requestApi, RequestHandler<T, R> requestHandler) {
		RequestHandler preRequestHandler = registry.put(requestApi, requestHandler);
		if (preRequestHandler != null) {
			log.warn("RequestHandler for {} already registered, overwrite it:{}", requestApi, preRequestHandler);
		}
	}

}
