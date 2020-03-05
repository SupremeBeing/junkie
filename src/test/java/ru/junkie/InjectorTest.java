/*
 * Copyright (c) 2019, Dmitriy Shchekotin
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

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class InjectorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullValueBind() {
        Injector injector = new Injector();
        injector.bind(List.class, (ArrayList<Object>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullClassBind() {
        Injector injector = new Injector();
        injector.bind(List.class, (Class<List<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTypeBind() {
        Injector injector = new Injector();
        ArrayList<Object> list = new ArrayList<>();
        injector.bind(null, list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullNameBind() {
        Injector injector = new Injector();
        ArrayList<Object> list = new ArrayList<>();
        injector.bind(List.class, null, list);
    }

    @Test
    public void testGetDefaultInstance() {
        Injector injector = new Injector();
        ArrayList<Object> list = new ArrayList<>();
        injector.bind(List.class, list);
        List<?> retrieved = injector.getInstance(List.class);
        Assert.assertNotNull(retrieved);
        Assert.assertSame(list, retrieved);
    }

    @Test
    public void testGetNamedInstance() {
        Injector injector = new Injector();
        ArrayList<Object> list = new ArrayList<>();
        ArrayList<Object> namedList = new ArrayList<>();
        injector.bind(List.class, list);
        injector.bind(List.class, "myList", namedList);
        List<?> retrievedNamed = injector.getInstance(List.class, "myList");
        Assert.assertNotNull(retrievedNamed);
        Assert.assertSame(namedList, retrievedNamed);
        Assert.assertNotSame(list, retrievedNamed);
    }

    @Test
    public void testGetDefaultType() {
        Injector injector = new Injector();
        injector.bind(List.class, ArrayList.class);
        List<?> retrieved1 = injector.getInstance(List.class);
        List<?> retrieved2 = injector.getInstance(List.class);
        Assert.assertNotNull(retrieved1);
        Assert.assertNotNull(retrieved2);
        Assert.assertNotSame(retrieved1, retrieved2);
    }

    @Test
    public void testGetNamedType() {
        Injector injector = new Injector();
        injector.bind(List.class, ArrayList.class);
        injector.bind(List.class, "myList", LinkedList.class);
        List<?> retrievedNamed = injector.getInstance(List.class, "myList");
        Assert.assertTrue(retrievedNamed instanceof LinkedList);
    }

    @Test
    public void testUnbind() {
        Injector injector = new Injector();
        injector.bind(List.class, ArrayList.class);
        injector.unbind(List.class);
        List<?> retrieved = injector.getInstance(List.class);
        Assert.assertNull(retrieved);
    }

    @Test
    public void testGetDefaultSupertype() {
        Injector injector = new Injector();
        injector.bind(List.class, ArrayList.class);
        Collection<?> retrieved = injector.getInstance(Collection.class);
        Assert.assertTrue(retrieved instanceof ArrayList);
    }

    @Test
    public void testGetDefaultInterface() {
        Injector injector = new Injector();
        injector.bind(List.class, List.class);
        Collection<?> collection = injector.getInstance(Collection.class);
        Assert.assertNull(collection);
    }

    @Test
    public void testGetDefaultSubtype() {
        Injector injector = new Injector();
        injector.bind(Collection.class, ArrayList.class);
        Collection<?> retrieved = injector.getInstance(List.class);
        Assert.assertNull(retrieved);
    }

    @Test
    public void testRebind() {
        Injector injector = new Injector();
        injector.bind(List.class, ArrayList.class);
        injector.bind(Collection.class, LinkedList.class);
        Collection<?> retrieved = injector.getInstance(Collection.class);
        Assert.assertTrue(retrieved instanceof List);
    }

    @Test
    public void testGetPrimitiveInstance() {
        Injector injector = new Injector();
        injector.bind(int.class, 10);
        Integer value = injector.getInstance(int.class);
        Assert.assertNotNull(value);
        Assert.assertEquals(10, value.intValue());
    }

    @Test
    public void testGetPrimitiveFromInstance() {
        Injector injector = new Injector();
        injector.bind(Integer.class, 10);
        Integer value = injector.getInstance(int.class);
        Assert.assertNotNull(value);
        Assert.assertEquals(10, value.intValue());
    }

    @Test
    public void testGetPrimitiveFromInstanceReversed() {
        Injector injector = new Injector();
        injector.bind(int.class, 10);
        Integer value = injector.getInstance(Integer.class);
        Assert.assertNotNull(value);
        Assert.assertEquals(10, value.intValue());
    }

    @Test
    public void testInstantiate() {
        Injector injector = new Injector();
        injector.bind(Integer.class, 10);
        List<String> list = new ArrayList<>();
        list.add("string");
        injector.bind(List.class, list);
        InjectMe injectMe = injector.instantiate(InjectMe.class);
        Assert.assertNotNull(injectMe);
        Assert.assertEquals(10, injectMe.getNumber());
        Assert.assertEquals(list, injectMe.getCollection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInstantiate() {
        Injector injector = new Injector();
        injector.instantiate(null);
    }

    @Test
    public void testInvoke() throws NoSuchMethodException {
        Injector injector = new Injector();
        injector.bind(String.class, "message");
        List<String> list = new ArrayList<>();
        list.add("string");
        injector.bind(List.class, list);
        InjectMe injectMe = new InjectMe(10, new ArrayList<>());
        injector.invoke(injectMe, InjectMe.class.getDeclaredMethod("setValues", String.class, Collection.class));
        Assert.assertEquals("message", injectMe.getMessage());
        Assert.assertEquals(list, injectMe.getCollection());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInvoke() {
        Injector injector = new Injector();
        InjectMe injectMe = new InjectMe(10, new ArrayList<>());
        injector.invoke(injectMe, null);
    }

    @Test
    public void testArray() {
        Injector injector = new Injector();
        int[] ar = injector.instantiate(int[].class);
        Assert.assertNotNull(ar);
    }
}
