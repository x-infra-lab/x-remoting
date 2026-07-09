package io.github.xinfra.lab.remoting.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ArraysUtils {

	public static byte[] concat(List<byte[]> byteLists) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			for (byte[] array : byteLists) {
				outputStream.write(array);
			}
			return outputStream.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				outputStream.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

}
