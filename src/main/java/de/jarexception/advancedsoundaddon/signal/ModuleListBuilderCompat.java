package de.jarexception.advancedsoundaddon.signal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleListBuilderCompat {
    private static final Map<Class<?>, Method> ADD_METHODS = new ConcurrentHashMap<>();

    private ModuleListBuilderCompat() {
    }

    public static void add(Object builder, Object module) {
        if (builder == null || module == null) {
            throw new IllegalArgumentException("builder and module are required");
        }
        Method method = ADD_METHODS.get(builder.getClass());
        if (method == null || !method.getParameterTypes()[0].isInstance(module)) {
            method = resolve(builder.getClass(), module.getClass());
            ADD_METHODS.put(builder.getClass(), method);
        }
        try {
            method.invoke(builder, module);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access DynamX module builder", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("DynamX module builder rejected the module", cause);
        }
    }

    private static Method resolve(Class<?> builderType, Class<?> moduleType) {
        for (Method method : builderType.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("add") && parameters.length == 1
                    && parameters[0].isAssignableFrom(moduleType)) {
                return method;
            }
        }
        throw new IllegalStateException("Compatible DynamX ModuleListBuilder.add method not found");
    }
}
