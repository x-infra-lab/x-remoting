package io.github.xinfra.lab.remoting.impl.handler;

import com.sun.corba.se.pept.protocol.ProtocolHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RequestHandlerRegistry {

	private ConcurrentHashMap<String, RequestHandler> registry = new ConcurrentHashMap<>();

	public <T, R> void register(RequestApi requestApi, RequestHandler<T, R> requestHandler) {
		RequestHandler preRequestHandler = registry.put(requestApi.path(), requestHandler);
		if (preRequestHandler != null) {
			log.warn("RequestHandler for {} already registered, overwrite it:{}", requestApi, preRequestHandler);
		}
	}

	public RequestHandler lookup(String path) {
		return registry.get(path);
	}

}
