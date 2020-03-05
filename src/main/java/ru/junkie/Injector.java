/*
 * Copyright (c) 2017, Dmitriy Shchekotin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ru.junkie;

import ru.reflexio.Primitive;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

// TODO: consider making thread-safe
public class Injector implements IInjector {

	private final Map<Class<?>, Map<String, Supplier<?>>> bindings = new HashMap<>();

	public Injector() {
		bind(IInjector.class, this);
	}

	@Override
	public <T> void bind(Class<T> type, String name, T implementation) {
		if (type == null || implementation == null || name == null) {
			throw new IllegalArgumentException();
		}
		getNamedBindings(type).put(name, () -> implementation);
	}

	@Override
	public <T> void bind(Class<T> type, T implementation) {
		bind(type, DEFAULT_NAME, implementation);
	}

	@Override
	public <T> void bind(Class<T> type, String name, Class<? extends T> implClass) {
		if (type == null || implClass == null || name == null) {
			throw new IllegalArgumentException();
		}
		// TODO: consider singleton implementation
		getNamedBindings(type).put(name, () -> instantiate(implClass));
	}

	private Map<String, Supplier<?>> getNamedBindings(Class<?> type) {
		return bindings.computeIfAbsent(type, k -> new HashMap<>());
	}

	@Override
	public <T> void bind(Class<T> type, Class<? extends T> implClass) {
		bind(type, DEFAULT_NAME, implClass);
	}

	@Override
	public void unbind(Class<?> type) {
		bindings.remove(type);
	}

	@Override
	public void unbindAll() {
		bindings.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getInstance(Class<T> type, String name) {
		if (type == null || name == null) {
			throw new IllegalArgumentException();
		}
		Set<Class<?>> boundTypes = new HashSet<>(bindings.keySet());
		for (Class<?> boundType : boundTypes) {
			if (type.isAssignableFrom(boundType) || Primitive.exists(type, boundType) || Primitive.exists(boundType, type)) {
				Map<String, Supplier<?>> namedBindings = bindings.get(boundType);
				if (namedBindings != null) {
					Supplier<?> supplier = namedBindings.get(name);
					if (supplier != null) {
						return (T) supplier.get();
					}
				}
			}
		}
		return null;
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return getInstance(type, DEFAULT_NAME);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T instantiate(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		if (type.isArray()) {
			return (T) Array.newInstance(type.getComponentType(), 0);
		} else {
			Constructor<T> ctor = findDefaultConstructor(type);
			return ctor == null ? null : invoke(ctor);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Constructor<T> findDefaultConstructor(Class<T> type) {
		Constructor<?> least = null;
		for (Constructor<?> ctor : type.getDeclaredConstructors()) {
			if (ctor.getParameterCount() == 0) {
				return (Constructor<T>) ctor;
			}
			if (least == null || ctor.getParameterCount() < least.getParameterCount()) {
				least = ctor;
			}
		}
		return (Constructor<T>) least;
	}

	private <T> T invoke(Constructor<T> ctor) {
		Object[] args = createArguments(ctor);
		return forceAccess(() -> {
			try {
				return ctor.newInstance(args);
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new RuntimeException(e);
			}
		}, ctor);
	}

	@Override
	public Object invoke(Object data, Method method) {
		if (method == null) {
			throw new IllegalArgumentException();
		}
		Object[] args = createArguments(method);
		return forceAccess(() -> {
			try {
				return method.invoke(data, args);
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}, method);
	}

	private Object[] createArguments(Executable executable) {
		Parameter[] params = executable.getParameters();
		Object[] result = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			Parameter param = params[i];
			InjectNamed in = param.getAnnotation(InjectNamed.class);
			result[i] = getInstance(param.getType(), in == null ? DEFAULT_NAME : in.value());
		}
		return result;
	}

	private <R> R forceAccess(Supplier<R> supplier, AccessibleObject element) {
		boolean accessible = element.isAccessible();
		if (!accessible) {
			element.setAccessible(true);
		}
		try {
			return supplier.get();
		} finally {
			if (!accessible) {
				element.setAccessible(false);
			}
		}
	}
}
