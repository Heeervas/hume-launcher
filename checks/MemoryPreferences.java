package com.petrobot.hume;

import android.content.SharedPreferences;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** In-memory Android preferences for testing the production migration on a host JVM. */
final class MemoryPreferences implements InvocationHandler {
    final Map<String, Object> values = new HashMap<>();
    final SharedPreferences prefs = (SharedPreferences) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{SharedPreferences.class}, this);

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if (name.equals("contains")) return values.containsKey(args[0]);
        if (name.equals("getAll")) return new HashMap<>(values);
        if (name.startsWith("get")) return values.getOrDefault(args[0], args[1]);
        if (name.equals("edit")) return editor();
        throw new UnsupportedOperationException(name);
    }

    private SharedPreferences.Editor editor() {
        Map<String, Object> pending = new HashMap<>();
        Set<String> removed = new HashSet<>();
        return (SharedPreferences.Editor) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.startsWith("put")) {
                        Object value = args[1];
                        pending.put((String) args[0], value instanceof Set ? new HashSet<>((Set<?>) value) : value);
                        return proxy;
                    }
                    if (name.equals("remove")) {
                        removed.add((String) args[0]);
                        return proxy;
                    }
                    if (name.equals("apply") || name.equals("commit")) {
                        for (String key : removed) values.remove(key);
                        values.putAll(pending);
                        return name.equals("commit") ? true : null;
                    }
                    throw new UnsupportedOperationException(name);
                });
    }
}
