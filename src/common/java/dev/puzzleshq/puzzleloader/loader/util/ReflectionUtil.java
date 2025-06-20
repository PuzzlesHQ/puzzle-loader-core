package dev.puzzleshq.puzzleloader.loader.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Util class for performing reflection.
 *
 * @author Mr_Zombii
 * @since 1.0.0
 */
public class ReflectionUtil {

    /**
     * Gets the field from the object.
     * @param o the object to get the field from.
     * @param fieldName the name of the field.
     * @throws NoSuchFieldException if not found.
     */
    public static Field getField(Object o, String fieldName) throws NoSuchFieldException {
        return getField(o.getClass(), fieldName);
    }

    /**
     * Gets the method from the object.
     * @param o the object to get the method from.
     * @param methodName the name of the method.
     * @param parameterTypes the parameter types of the method
     * @throws NoSuchMethodException if not found.
     */
    public static Method getMethod(Object o, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getMethod(o.getClass(), methodName, parameterTypes);
    }

    /**
     * Gets the constructor from the object.
     * @param o the object to get the constructor from.
     * @param parameterTypes the parameter types of the constructor
     * @throws NoSuchMethodException if not found.
     */
    public static Constructor<?> getConstructor(Object o, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getConstructor(o.getClass(), parameterTypes);
    }

    /**
     * Gets the field from the Class.
     * @param clazz the Class to get the field from.
     * @param fieldName the name of the field.
     * @throws NoSuchFieldException if not found.
     */
    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field;
        try {
            field = clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            field = clazz.getDeclaredField(fieldName);
        }
        field.setAccessible(true);
        return field;
    }

    /**
     * Gets the method from the Class.
     * @param clazz the Class to get the method from.
     * @param methodName the name of the method.
     * @param parameterTypes the parameter types of the method
     * @throws NoSuchMethodException if not found.
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method;
        try {
            method = clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        }
        method.setAccessible(true);
        return method;
    }

    /**
     * Gets the constructor from the Class.
     * @param clazz the Class to get the constructor from.
     * @param parameterTypes the parameter types of the constructor
     * @throws NoSuchMethodException if not found.
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) throws NoSuchMethodException {
        Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            constructor = clazz.getDeclaredConstructor(parameterTypes);
        }
        constructor.setAccessible(true);
        return constructor;
    }

}
