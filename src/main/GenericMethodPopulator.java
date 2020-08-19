package main;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class GenericMethodPopulator {
    private final Map<Class, Map<String, FunctionBinder>> functionStorage = new HashMap<>();

    private static final Lookup LOOKUP = MethodHandles.lookup();

    public <S, T> void populate(final Class<S> sourceClass, final S source, final Class<T> targetClass, final T target) {
        final Map<String, Method> sourceFields = Arrays.stream(sourceClass.getDeclaredMethods())
                .collect(toMap(Method::getName, identity()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("get"))
                .map(entry -> entry(entry.getKey().replace("get", ""), entry.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        sourceFields.entrySet().forEach(entry -> populateEntry(sourceClass, source, targetClass, target, entry));
    }

    private <S, T, U> void populateEntry(final Class<S> sourceClass, final S source, final Class<T> targetClass,
                                   final T target, final Entry<String, Method> sourceEntry) {
        try {
            final MethodHandle setter = findMethod(targetClass, sourceEntry, "set",
                    sourceEntry.getValue().getReturnType(), null);
            final Consumer<U> consumer = (Consumer) setter.bindTo(target).invoke();

            final MethodHandle getter = findMethod(sourceClass, sourceEntry, "get", null, sourceEntry.getValue());
            final Supplier<U> supplier = (Supplier) getter.bindTo(source).invoke();

            consumer.accept(supplier.get());
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    private MethodHandle findMethod(final Class<?> clazz, final Entry<String, Method> entry,
                                    final String prefix, final Class<?> parameter, final Method method) {
        return Optional.of(functionStorage)
                .map(map -> map.get(clazz))
                .map(map -> map.get(entry.getKey()))
                .map(functionBinder -> prefix.equals("set") ? functionBinder.getSetter() : functionBinder.getGetter())
                .orElseGet(() -> getMethodHandle(clazz, entry.getKey(), prefix, parameter, method));
    }

    private MethodHandle getMethodHandle(final Class<?> clazz, final String fieldName, final String prefix,
                                         final Class<?> parameter, final Method method) {
        try {
            final Method realMethod = method != null ? method : clazz.getDeclaredMethod(prefix + fieldName, parameter);
            final MethodHandle methodHandle = createFromMethod(realMethod, prefix.equals("set") ? Consumer.class : Supplier.class);
            setFunctionMap(clazz, fieldName, methodHandle,
                    prefix.equals("set") ? FunctionBinder::setSetter : FunctionBinder::setGetter);
            return methodHandle;
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MethodHandle createFromMethod(final Method method, final Class<?> type) {
        try {
            final MethodHandle methodHandle = LOOKUP.unreflect(method);
            return LambdaMetafactory.metafactory(LOOKUP, type.equals(Consumer.class) ? "accept" : "get",
                    MethodType.methodType(type), methodHandle.type(), methodHandle, methodHandle.type()).getTarget();
        } catch (final IllegalAccessException | LambdaConversionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setFunctionMap(final Class<?> clazz, final String fieldName, final MethodHandle methodHandle,
                                final BiConsumer<FunctionBinder, MethodHandle> methodType) {
        final Map<String, FunctionBinder> classFields = functionStorage.get(clazz);
        if (classFields != null) {
            FunctionBinder functionBinder = classFields.get(fieldName);
            if (functionBinder != null) {
                methodType.accept(functionBinder, methodHandle);
            } else {
                functionBinder = new FunctionBinder();
                methodType.accept(functionBinder, methodHandle);
                classFields.put(fieldName, functionBinder);
            }
        } else {
            final FunctionBinder functionBinder = new FunctionBinder();
            methodType.accept(functionBinder, methodHandle);
            functionStorage.put(clazz, Map.of(fieldName, functionBinder));
        }
    }
}
