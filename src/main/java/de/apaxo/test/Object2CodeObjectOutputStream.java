/**
 * 
 */
package de.apaxo.test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class serializes a java bean to java code.
 * 
 * It supports all primitive types, object cycles, Collections and Maps.
 * 
 * Generic information will be lost during the serialization.
 * 
 * @author Manuel Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
public class Object2CodeObjectOutputStream implements AutoCloseable {

    private static final Logger log = Logger
            .getLogger(Object2CodeObjectOutputStream.class.getName());

    private OutputStream out;

    public Object2CodeObjectOutputStream(OutputStream out) throws IOException,
            SecurityException {
        this.out = out;
    }
    
    public void writeObject(Object o) {
    	writeObject(o, false);
    }

    /**
     * Writes an object into the given output stream.
     */
    public void writeObject(Object o, boolean onlyPropertiesWithMatchingField) {
        if (o == null) {
            log.warning("Given object is null.");
            return;
        }
        // map that counts how many instances of a certain
        // clazz we already generated.
        Map<Class<?>, Integer> clazz2count = new HashMap<Class<?>, Integer>();
        // this map contains a mapping from objects to their assigned names
        Map<Object, String> object2variableName = new HashMap<Object, String>();
        String name = writeObject(o, clazz2count, object2variableName, onlyPropertiesWithMatchingField);
        Class<?> clazz = o.getClass();
        if (isPrimitiveOrBoxClass(clazz)) {
            // write the simple type to the stream
            try {
                out.write(name.getBytes());
            } catch (IOException e) {
                log.log(Level.WARNING, "Exception was thrown", e);
            }
        }
    }

    /**
     * Checks if the clazz is a primitive class or if it is a boxed class.
     * 
     * @param clazz
     * @return
     */
    private boolean isPrimitiveOrBoxClass(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == Integer.class
                || clazz == Byte.class || clazz == Boolean.class
                || clazz == Short.class || clazz == Long.class
                || clazz == Double.class || clazz == Float.class
                || clazz == Character.class || clazz == String.class;
    }

    private String writeObject(Object o, Map<Class<?>, Integer> clazz2count,
            Map<Object, String> object2variableName, boolean onlyPropertiesWithMatchingField) {
        try {
            Class<?> clazz = o.getClass();
            // write primitive types directly out
            if (clazz.isPrimitive() || clazz == Integer.class
                    || clazz == Byte.class || clazz == Boolean.class
                    || clazz == Short.class || clazz == Long.class
                    || clazz == Double.class || clazz == Float.class
                    || clazz == Character.class) {
                return formatType(clazz, o);
            } else if (clazz == String.class) {
                return "\"" + o.toString() + "\"";
            }
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            // This should throw an exception if there is no
            // args constructor
            clazz.getConstructor();
            String beanName = getVariableName(clazz, clazz2count);
            object2variableName.put(o, beanName);
            out.write((clazz.getName() + " " + beanName + " = new "
                    + clazz.getName() + "();\n").getBytes());
            for (PropertyDescriptor propertyDescriptor : beanInfo
                    .getPropertyDescriptors()) {
            	if (onlyPropertiesWithMatchingField) {
            		try {
    					clazz.getDeclaredField(propertyDescriptor.getName());
    				} catch (NoSuchFieldException e) {
    					log.info("Skipping method without matching field: " + propertyDescriptor.getName());
    					continue;
    				}
            	}
                Class<?> propertyClass = propertyDescriptor.getPropertyType();
                Object propertyValue = propertyDescriptor.getReadMethod()
                        .invoke(o);
                if (propertyValue != null) {
                    if (propertyClass.isPrimitive()) {
                        Method writeMethod = propertyDescriptor
                                .getWriteMethod();
                        if (writeMethod != null) {
                            out.write((beanName + "." + writeMethod.getName()
                                    + "("
                                    + formatType(propertyClass, propertyValue) + ");\n")
                                    .getBytes());
                        } else {
                            log.warning("Can not find write method for: "
                                    + o.getClass().getName() + " "
                                    + propertyDescriptor.getName());
                        }
                    } else if (propertyClass == String.class) {
                        out.write((beanName + "."
                                + propertyDescriptor.getWriteMethod().getName()
                                + "(\"" + propertyValue + "\");\n").getBytes());
                    } else {
                        if (Collection.class.isAssignableFrom(propertyClass)) {
                            Collection<?> collection = (Collection<?>) propertyValue;
                            Class<?> collectionImplementation = collection
                                    .getClass();
                            String collectionName = getVariableName(
                                    collectionImplementation, clazz2count);
                            object2variableName.put(collection, collectionName);
                            out.write((collectionImplementation.getName() + " "
                                    + collectionName + " = new "
                                    + collectionImplementation.getName() + "();\n")
                                    .getBytes());
                            for (Object item : collection) {
                                String itemName = writeObject(item,
                                        clazz2count, object2variableName, onlyPropertiesWithMatchingField);
                                out.write((collectionName + ".add(" + itemName + ");\n")
                                        .getBytes());
                            }
                            out.write((beanName
                                    + "."
                                    + propertyDescriptor.getWriteMethod()
                                            .getName() + "(" + collectionName + ");\n")
                                    .getBytes());
                        } else if (Map.class.isAssignableFrom(propertyClass)) {
                            Map<?, ?> map = (Map<?, ?>) propertyValue;
                            Class<?> mapImplementation = map.getClass();
                            String mapName = getVariableName(mapImplementation,
                                    clazz2count);
                            object2variableName.put(map, mapName);
                            out.write((mapImplementation.getName() + " "
                                    + mapName + " = new "
                                    + mapImplementation.getName() + "();\n")
                                    .getBytes());
                            for (Entry<?, ?> item : map.entrySet()) {
                                String keyName = object2variableName
                                        .containsKey(item.getKey()) ? object2variableName
                                        .get(item.getKey()) : writeObject(
                                        item.getKey(), clazz2count,
                                        object2variableName, onlyPropertiesWithMatchingField);
                                String valueName = object2variableName
                                        .containsKey(item.getValue()) ? object2variableName
                                        .get(item.getValue()) : writeObject(
                                        item.getValue(), clazz2count,
                                        object2variableName, onlyPropertiesWithMatchingField);

                                out.write((mapName + ".put(" + keyName + ", "
                                        + valueName + ");\n").getBytes());
                            }
                            out.write((beanName
                                    + "."
                                    + propertyDescriptor.getWriteMethod()
                                            .getName() + "(" + mapName + ");\n")
                                    .getBytes());
                        } else if (propertyClass != Class.class) {
                            String newBeanName = object2variableName
                                    .containsKey(propertyValue) ? object2variableName
                                    .get(propertyValue) : writeObject(
                                    propertyValue, clazz2count,
                                    object2variableName, onlyPropertiesWithMatchingField);

                            Method writeMethod = propertyDescriptor
                                    .getWriteMethod();
                            if (writeMethod != null) {
                                out.write((beanName
                                        + "."
                                        + propertyDescriptor.getWriteMethod()
                                                .getName() + "(" + newBeanName + ");\n")
                                        .getBytes());
                            } else {
                                log.warning("Can not find write method for: "
                                        + o.getClass().getName() + " "
                                        + propertyDescriptor.getName());
                            }

                        }
                    }
                }
            }
            return beanName;
        } catch (IntrospectionException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (SecurityException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (IllegalAccessException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (IllegalArgumentException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (InvocationTargetException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        }
        throw new RuntimeException(
                "Could not serialize the given object to code. Please see the warnings in the log.");
    }

    /**
     * Formats the given value according to the given class.
     * 
     * @param clazz
     *            primitive class to use for formatting.
     * @param value
     *            the value of the variable
     * @return a string representation of the variable
     */
    private String formatType(Class<?> clazz, Object value) {
        if (clazz == Byte.TYPE || clazz == Byte.class) {
            return "(byte) " + value;
        } else if (clazz == Character.TYPE || clazz == Character.class) {
            return "'" + value + "'";
        } else if (clazz == Short.TYPE || clazz == Short.class) {
            return "(short)" + value;
        } else if (clazz == Long.TYPE || clazz == Long.class) {
            return value + "l";
        } else if (clazz == Float.TYPE || clazz == Float.class) {
            return value + "f";
        } else if (clazz == Boolean.TYPE || clazz == Boolean.class) {
            return value.toString();
        } else if (clazz == Double.TYPE || clazz == Double.class) {
            return value.toString();
        } else if (clazz == Integer.TYPE || clazz == Integer.class) {
            return value.toString();
        } else {
            throw new IllegalArgumentException("Type " + clazz.getName()
                    + " is not a supported primitive type.");
        }
    }

    /**
     * Get a variable name for the given class this also makes sure if a certain
     * class has multiple instance they get different names.
     * 
     * @param clazz
     *            the class for which we need a new name
     * @param clazz2count
     *            how many instances of the classes do we already have
     */
    private String getVariableName(Class<?> clazz,
            Map<Class<?>, Integer> clazz2count) {
        int count = 0;
        if (clazz2count.containsKey(clazz)) {
            count = clazz2count.get(clazz);
            count++;
            clazz2count.put(clazz, count);
        } else {
            clazz2count.put(clazz, count);
        }
        return Introspector.decapitalize(clazz.getSimpleName()) + count;
    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        }
    }

}
