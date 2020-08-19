package main;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.stream.Collectors;

public class Main {
    private static final int ITERATIONS = 50_000_000;
    private static final int WARM_UP = 0;
    private static int test = 0;


    public static void main(String[] args) {
        Person person = new Person();
        person.setName("name1");
        person.setAge(2);
        person.setCountry("country1");
        person.setStreet("street1");
        person.setHouseNumber(13);
        final Person person2 = new Person();

        final GenericMethodPopulator genericMethodPopulator = new GenericMethodPopulator();

        genericMethodPopulator.populate(Person.class, person, Person.class, person2);
        System.out.println(person2.getName());
    }

    public static void egal() throws Throwable {
        // hold result to prevent too much optimizations
        final int[] dummy = new int[4];


        long testStart = System.nanoTime();
        final Field test = Main.class.getDeclaredField("test");
        test.setAccessible(true);
        test.set(null, 5);
        final Object o = test.get(null);
        long testEnd = System.nanoTime();
        System.out.println(o);

        long buildStart = System.nanoTime();
        Method reflected = Main.class.getDeclaredMethod("myMethod", int.class, int.class);
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle mh = lookup.unreflect(reflected);
        final MethodHandle applyAsInt = LambdaMetafactory.metafactory(
                lookup, "applyAsInt", MethodType.methodType(IntBinaryOperator.class),
                mh.type(), mh, mh.type()).getTarget();
        long buildMid = System.nanoTime();

        IntBinaryOperator lambda = (IntBinaryOperator) applyAsInt.invokeExact();
        long buildEnd = System.nanoTime();

        for (int i = 0; i < WARM_UP; i++) {
            dummy[0] += testDirect(dummy[0]);
            dummy[1] += testLambda(dummy[1], lambda);
            dummy[2] += testMH(dummy[1], mh);
            dummy[3] += testReflection(dummy[2], reflected);
        }
        long t0 = System.nanoTime();
        dummy[0] += testDirect(dummy[0]);
        long t1 = System.nanoTime();
        dummy[1] += testLambda(dummy[1], lambda);
        long t2 = System.nanoTime();
        dummy[2] += testMH(dummy[1], mh);
        long t3 = System.nanoTime();
        dummy[3] += testReflection(dummy[2], reflected);
        long t4 = System.nanoTime();
        print(Map.of("direct", t1 - t0,
                "lambda", t2 - t1,
                "methodHandle", t3 - t2,
                "reflection", t4 - t3,
                "build", buildEnd -buildStart,
                "buildStartToMid", buildMid -buildStart,
                "buildMidToEnd", buildEnd -buildMid,
                "field", testEnd - testStart));

        // do something with the results
        if (dummy[0] != dummy[1] || dummy[0] != dummy[2] || dummy[0] != dummy[3])
            throw new AssertionError();
    }

    private static void print(final Map<String, Long> times) {
        final String result = times.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(entry -> entry.getKey() + String.format(": %.2fms", entry.getValue() * 1e-6))
                .collect(Collectors.joining(System.lineSeparator()));
        System.out.println(result);
    }

    private static int testMH(int v, MethodHandle mh) throws Throwable {
        for (int i = 0; i < ITERATIONS; i++)
            v += (int) mh.invokeExact(1000, v);
        return v;
    }

    private static int testReflection(int v, Method mh) throws Throwable {
        for (int i = 0; i < ITERATIONS; i++)
            v += (int) mh.invoke(null, 1000, v);
        return v;
    }

    private static int testDirect(int v) {
        for (int i = 0; i < ITERATIONS; i++)
            v += myMethod(1000, v);
        return v;
    }

    private static int testLambda(int v, IntBinaryOperator accessor) {
        for (int i = 0; i < ITERATIONS; i++)
            v += accessor.applyAsInt(1000, v);
        return v;
    }

    private static int myMethod(int a, int b) {
        return a < b ? a : b;
    }
}
