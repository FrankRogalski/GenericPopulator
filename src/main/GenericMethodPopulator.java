package main;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * awesome sources:
 * https://stackoverflow.com/questions/27602758/java-access-bean-methods-with-lambdametafactory
 * https://stackoverflow.com/questions/19557829/faster-alternatives-to-javas-reflection#19563000
 */
@SuppressWarnings({"rawtypes", "unchecked", "RedundantExplicitVariableType", "java:S106", "java:S3740"})
public class GenericMethodPopulator {
    private static final String SET = "set";
    private static final String ACCEPT = "accept";

    private final ConcurrentHashMap<Class, Map<String, BiConsumer>> methods = new ConcurrentHashMap<>();

    private static final Lookup LOOKUP = MethodHandles.lookup();

    public void populate(final Map<String, Object> values, final Class targetClass, final Object target) {
        final Map<String, BiConsumer> methodMap = methods.computeIfAbsent(targetClass, this::getMethods);

        values.entrySet().stream()
                .filter(this::isNewer)
                .forEach(entry -> methodMap.get(entry.getKey()).accept(target, entry.getValue()));
    }

    private Map<String, BiConsumer> getMethods(final Class targetClass) {
        return stream(targetClass.getDeclaredMethods())
                .collect(toMap(Method::getName, identity()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(SET))
                .collect(toMap(this::toAttributeName, this::convertToLambda));
    }

    private String toAttributeName(Entry<String, Method> entry) {
        final String method = entry.getKey().replace(SET, "");
        return method.substring(0, 1).toLowerCase() + method.substring(1);
    }

    private boolean isNewer(final Entry<String, Object> entry) {
        return !entry.getKey().equals("fish"); //check if newer with the single call to Dao in populate
    }

    private BiConsumer convertToLambda(final Entry<String, Method> entry) {
        try {
            final Method method = entry.getValue();
            final MethodType setter = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            final MethodHandle methodHandle = LOOKUP.findVirtual(method.getDeclaringClass(), method.getName(), setter);
            final MethodType methodType = methodHandle.type();
            final CallSite callSite = LambdaMetafactory.metafactory(LOOKUP, ACCEPT,
                    MethodType.methodType(BiConsumer.class), methodType, methodHandle, methodType);
            return (BiConsumer) callSite.getTarget().invoke();
        } catch (final Throwable ex) {
            System.err.print("fuck: ");
            System.err.println(ex.getMessage());
            throw new IllegalArgumentException();
        }
    }
}
