package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageHeader;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RpcMessageHeader implements Serializable, MessageHeader {

	private List<Item> items = new ArrayList<>();

	public void addItem(Item item) {
		items.add(item);
	}

	@Override
	public void serialize() throws SerializeException {
		// todo
	}

	@Override
	public void deserialize() throws DeserializeException {
		// todo
	}

	@Setter
	@Getter
	@NoArgsConstructor
	@EqualsAndHashCode
	public static class Item implements Serializable {

		private String key;

		private List<String> values;

		public Item(String key, String value) {
			this.key = key;
			this.values = new ArrayList<>(1);
			values.add(value);
		}

	}

}
