package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Responses {

	public static void sendResponse(Connection connection, ResponseMessage responseMessage) {
		// todo
		try {
			responseMessage.serialize();
		}
		catch (SerializeException e) {
			log.error("responseMessage serialize ex", e);

		}
	}

}
