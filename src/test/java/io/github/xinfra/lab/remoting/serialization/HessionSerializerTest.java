package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

public class HessionSerializerTest {

	@Test
	public void testSerialize() throws SerializeException, DeserializeException {
		HessionSerializer serializer = new HessionSerializer();

		User user = new User();
		user.setName("joe");
		user.setAge((short) 29);

		byte[] data = serializer.serialize(user);

		User deUser = serializer.deserialize(data, User.class);
		Assertions.assertEquals(user, deUser);

		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		deUser = serializer.deserialize(data, User.class);
		Assertions.assertEquals(user, deUser);
	}

	@Data
	@EqualsAndHashCode
	public static class User implements Serializable {

		private String name;

		private short age;

	}

}
