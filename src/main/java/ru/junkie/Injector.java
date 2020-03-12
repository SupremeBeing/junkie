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

import ru.reflexio.IConstructorReflection;
import ru.reflexio.IExecutableReflection;
import ru.reflexio.IInstanceMethodReflection;
import ru.reflexio.IParameterReflection;
import ru.reflexio.IStaticMethodReflection;
import ru.reflexio.ITypeReflection;
import ru.reflexio.Primitive;
import ru.reflexio.TypeReflection;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

public class Injector implements IInjector {

	private final Map<Class<?>, Map<String, Supplier<?>>> bindings = new HashMap<>();

	public Injector() {
		bind(IInjector.class, this);
	}

	@Override
	public <T> void bind(Class<T> type, String name, T implementation) {
		if (type == null || implementation == null) {
			throw new IllegalArgumentException();
		}
		bindings.computeIfAbsent(type, k -> new HashMap<>()).put(name, () -> implementation);
	}

	@Override
	public <T> void bind(Class<T> type, T implementation) {
		bind(type, null, implementation);
	}

	@Override
	public <T> void bind(Class<T> type, String name, Class<? extends T> implClass) {
		if (type == null || implClass == null) {
			throw new IllegalArgumentException();
		}
		bindings.computeIfAbsent(type, k -> new HashMap<>()).put(name, () -> instantiate(implClass));
	}

	@Override
	public <T> void bind(Class<T> type, Class<? extends T> implClass) {
		bind(type, null, implClass);
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
		if (type == null) {
			throw new IllegalArgumentException();
		}
		for (Class<?> boundType : bindings.keySet()) {
			if (type.isAssignableFrom(boundType) || Primitive.canAssign(type, boundType)) {
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
		return getInstance(type, null);
	}

	@Override
	public <T> T instantiate(Class<T> type) {
		ITypeReflection<T> tr = new TypeReflection<>(type);
		IConstructorReflection<T> ctor = tr.findDefaultConstructor();
		return ctor == null ? null : ctor.invoke(createArguments(ctor));
	}

	@Override
	public Object invoke(Object data, IInstanceMethodReflection method) {
		if (method == null || data == null) {
			throw new IllegalArgumentException();
		}
		return method.invoke(data, createArguments(method));
	}

	@Override
	public Object invoke(IStaticMethodReflection method) {
		if (method == null) {
			throw new IllegalArgumentException();
		}
		return method.invoke(createArguments(method));
	}

	private Object[] createArguments(IExecutableReflection executable) {
		List<IParameterReflection> params = executable.getParameters();
		Object[] result = new Object[params.size()];
		for (int i = 0; i < params.size(); i++) {
			IParameterReflection param = params.get(i);
			InjectNamed in = param.getAnnotation(InjectNamed.class);
			result[i] = getInstance(param.getType(), in == null ? null : in.value());
		}
		return result;
	}

}
