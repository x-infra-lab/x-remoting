package io.github.xinfra.lab.remoting.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallOptions {

	private int timeoutMills = 3000;

}
