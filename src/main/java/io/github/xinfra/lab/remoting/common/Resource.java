package io.github.xinfra.lab.remoting.common;

public interface Resource<T> {

	T get();

	void close();

}