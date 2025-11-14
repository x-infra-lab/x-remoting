package io.github.xinfra.lab.remoting.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallOptions {

	@Builder.Default
	private int timeoutMills = 3000;

}
