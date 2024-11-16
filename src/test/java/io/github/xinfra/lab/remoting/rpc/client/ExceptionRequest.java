package io.github.xinfra.lab.remoting.rpc.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionRequest implements Serializable {

	private String errorMsg;

}
