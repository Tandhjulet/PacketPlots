package net.dashmc.plots.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MethodWrapper<T> {
	public final Object instance;
	public final Method method;

	@SuppressWarnings("unchecked")
	public T call(Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return (T) method.invoke(instance, args);
	}
}
