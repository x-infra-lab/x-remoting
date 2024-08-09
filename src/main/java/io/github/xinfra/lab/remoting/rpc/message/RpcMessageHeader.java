package io.github.xinfra.lab.remoting.rpc.message;

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
public class RpcMessageHeader implements Serializable {

	private List<Item> items = new ArrayList<>();

	public void addItem(Item item) {
		items.add(item);
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
