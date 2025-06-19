package io.github.xinfra.lab.remoting.serialization;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianFactory;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessionSerializer implements Serializer {

	HessianFactory hessianFactory = new HessianFactory();

	// todo: thinking `localOutputByteArray` occupy a large space
	private static ThreadLocal<ByteArrayOutputStream> localOutputByteArray = new ThreadLocal<ByteArrayOutputStream>() {
		@Override
		protected ByteArrayOutputStream initialValue() {
			return new ByteArrayOutputStream();
		}
	};

	@Override
	public byte[] serialize(Object obj) throws SerializeException {
		ByteArrayOutputStream baos = localOutputByteArray.get();
		baos.reset();

		Hessian2Output hessian2Output = null;
		try {
			hessian2Output = hessianFactory.createHessian2Output(baos);
			hessian2Output.writeObject(obj);
			hessian2Output.close();
		}
		catch (IOException e) {
			throw new SerializeException("serialize fail.", e);
		}
		finally {
			hessianFactory.freeHessian2Output(hessian2Output);
		}

		return baos.toByteArray();
	}

	@Override
	public <T> T deserialize(byte[] data, Class<T> clazz) throws DeserializeException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		Hessian2Input hessian2Input = null;
		Object obj;
		try {
			hessian2Input = hessianFactory.createHessian2Input(bais);
			obj = hessian2Input.readObject();
			hessian2Input.close();
		}
		catch (IOException e) {
			throw new DeserializeException("deserialize fail.", e);
		}
		finally {
			hessianFactory.freeHessian2Input(hessian2Input);
		}
		return (T) obj;
	}

}
