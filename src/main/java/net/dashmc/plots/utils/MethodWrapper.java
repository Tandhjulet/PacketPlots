package net.dashmc.plots.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MethodWrapper<T> {
	private final Object instance;
	private final Method method;

	public MethodWrapper(Object instance, String methodName, Class<?>... params)
			throws NoSuchMethodException, SecurityException {
		this.instance = instance;
		method = instance.getClass().getDeclaredMethod(methodName, params);
		method.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	public T call(Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return (T) method.invoke(instance, args);
	}
}
