package main;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * awesome sources:
 * https://stackoverflow.com/questions/27602758/java-access-bean-methods-with-lambdametafactory
 * https://stackoverflow.com/questions/19557829/faster-alternatives-to-javas-reflection#19563000
 * https://github.com/const/sample-getters-setters/blob/master/src/main/java/samples/SettersJava11.java
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
            return createSetter(LOOKUP, methodHandle, method.getParameterTypes()[0]);
        } catch (final Exception ex) {
            throw new IllegalArgumentException();
        }
    }

    public static BiConsumer createSetter(final MethodHandles.Lookup lookup,
                                          final MethodHandle setter,
                                          final Class<?> valueType) throws Exception {
        try {
            if (valueType.isPrimitive()) {
                if (valueType == double.class) {
                    ObjDoubleConsumer consumer = (ObjDoubleConsumer) createSetterCallSite(
                            lookup, setter, ObjDoubleConsumer.class, double.class).getTarget().invokeExact();
                    return (a, b) -> consumer.accept(a, (double) b);
                } else if (valueType == int.class) {
                    ObjIntConsumer consumer = (ObjIntConsumer) createSetterCallSite(
                            lookup, setter, ObjIntConsumer.class, int.class).getTarget().invokeExact();
                    return (a, b) -> consumer.accept(a, (int) b);
                } else if (valueType == long.class) {
                    ObjLongConsumer consumer = (ObjLongConsumer) createSetterCallSite(
                            lookup, setter, ObjLongConsumer.class, long.class).getTarget().invokeExact();
                    return (a, b) -> consumer.accept(a, (long) b);
                } else {
                    // Real code needs to support short, char, boolean, byte, and float according to pattern above
                    throw new RuntimeException("Type is not supported yet: " + valueType.getName());
                }
            } else {
                return (BiConsumer) createSetterCallSite(lookup, setter, BiConsumer.class, Object.class)
                        .getTarget().invokeExact();
            }
        } catch (final Exception e) {
            throw e;
        } catch (final Throwable e) {
            throw new Error(e);
        }
    }

    private static CallSite createSetterCallSite(MethodHandles.Lookup lookup, MethodHandle setter, Class<?> interfaceType, Class<?> valueType) throws LambdaConversionException {
        return LambdaMetafactory.metafactory(lookup, ACCEPT, MethodType.methodType(interfaceType),
                MethodType.methodType(void.class, Object.class, valueType), //signature of method SomeConsumer.accept after type erasure
                setter, setter.type());
    }
}
