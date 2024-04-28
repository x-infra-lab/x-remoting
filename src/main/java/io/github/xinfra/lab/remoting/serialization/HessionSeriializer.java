package io.github.xinfra.lab.remoting.serialization;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessionSeriializer implements Serializer {

    private SerializerFactory serializerFactory = new SerializerFactory();
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

        Hessian2Output hessian2Output = new Hessian2Output(baos);
        try {
            hessian2Output.writeObject(obj);
            hessian2Output.close();
        } catch (IOException e) {
            throw new SerializeException("serialize fail.", e);
        }

        return baos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] data, String clazz) throws DeserializeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Hessian2Input hessian2Input = new Hessian2Input(bais);
        Object obj = null;
        try {
            obj = hessian2Input.readObject();
            hessian2Input.close();
        } catch (IOException e) {
            throw new DeserializeException("deserialize fail.", e);
        }
        return (T) obj;
    }
}
