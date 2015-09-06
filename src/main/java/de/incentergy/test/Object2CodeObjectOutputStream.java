/**
 * 
 */
package de.incentergy.test;

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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class serializes a java bean to java code.
 * 
 * It supports all primitive types, object cycles, Collections and Maps.
 * 
 * Generic information will be lost during the serialization.
 * 
 * @author Manuel Blechschmidt <manuel.blechschmidt@incentergy.de>
 * 
 */
public class Object2CodeObjectOutputStream implements AutoCloseable {

	private static final Logger log = Logger
			.getLogger(Object2CodeObjectOutputStream.class.getName());

	/**
	 * This map contains Functions that can generate constructors for classes
	 */
	private static Map<Class<?>, Function<Object, String>> class2constructorGenerator = new HashMap<>();

	/**
	 * This map can contain for a class a map that decides if only certain
	 * fields should be serialized. The fields are given as strings
	 */
	private static Map<Class<?>, Map<String, Boolean>> class2fieldIncludes = new HashMap<>();

	/**
	 * The OutputStream to write the object to.
	 */
	private OutputStream out;

	/**
	 * Generate a new Object2CodeObjectOutputStream based on an OutputStream
	 * e.g. Object2CodeObjectOutputStream oos = new
	 * Object2CodeObjectOutputStream(System.out); There is also a shortcut in
	 * {@link de.incentergy.test.Serialize#object2code(Object)}
	 * 
	 * @paramt out the outputstream to use to write the code into
	 */
	public Object2CodeObjectOutputStream(OutputStream out) throws IOException,
			SecurityException {
		this.out = out;
	}

	/**
	 * Only include this field for the class.
	 * 
	 * @param clazz
	 * @param field
	 */
	public static void includeFieldForClass(Class<?> clazz, String field) {
		Map<String, Boolean> includes = class2fieldIncludes.get(clazz);
		if(includes == null) {
			includes = new HashMap<>();
			class2fieldIncludes.put(clazz, includes);
		}
		includes.put(field, true);
	}
	
	/**
	 * Removes all the includes for the class.
	 * 
	 * @param clazz
	 */
	public static void removeAllIncludesForClass(Class<?> clazz) {
		class2fieldIncludes.remove(clazz);
	}
	
	/**
	 * Adds a custom constructor generator for a specified class.
	 * 
	 * @param clazz
	 *            the class typically without not args constructors
	 * @param function
	 *            the function to use
	 */
	@SuppressWarnings("unchecked")
	public static <T> void addCustomConstructorGenerator(Class<T> clazz,
			Function<T, String> function) {
		class2constructorGenerator.put(clazz,
				(Function<Object, String>) function);
	}

	/**
	 * Removes the custom constructor generator for the specified class.
	 * 
	 * @param clazz
	 */
	public static void removeCustomConstructorGenerator(Class<?> clazz) {
		class2constructorGenerator.remove(clazz);
	}

	/**
	 * Writes an object ussing all getters and setters to the given output
	 * stream.
	 * 
	 * @param o
	 *            the object to write
	 */
	public void writeObject(Object o) {
		writeObject(o, false);
	}

	/**
	 * Writes an object into the given output stream.
	 * 
	 * @param o
	 *            the Object to write
	 * @param onlyPropertiesWithMatchingField
	 *            use only fields that have a corresponding private field?
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
		String name = writeObject(o, clazz2count, object2variableName,
				onlyPropertiesWithMatchingField);
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
	 *            the class to check
	 */
	private boolean isPrimitiveOrBoxClass(Class<?> clazz) {
		return isPrimitiveOrBoxClass(clazz, true);
	}

	/**
	 * Checks if the clazz is a primitive class or if it is a boxed class.
	 * 
	 * @param clazz
	 *            the class to check
	 * @param checkString
	 *            handle String as primitive class
	 * @return is it a primitive or box class
	 */
	private boolean isPrimitiveOrBoxClass(Class<?> clazz, boolean checkString) {
		return clazz.isPrimitive() || clazz == Integer.class
				|| clazz == Byte.class || clazz == Boolean.class
				|| clazz == Short.class || clazz == Long.class
				|| clazz == Double.class || clazz == Float.class
				|| clazz == Character.class
				|| (clazz == String.class && checkString);
	}

	/**
	 * @see Object2CodeObjectOutputStream#writeObject(Object, Map, Map, boolean,
	 *      int, int)
	 */
	private String writeObject(Object o, Map<Class<?>, Integer> clazz2count,
			Map<Object, String> object2variableName,
			boolean onlyPropertiesWithMatchingField) {
		return writeObject(o, clazz2count, object2variableName,
				onlyPropertiesWithMatchingField, 0, 0);
	}

	/**
	 * Private function that is doing the recursive work.
	 * 
	 * @param o
	 *            the object to write
	 * @param clazz2count
	 *            Counting how often a certain class was already produced
	 * @param object2variableName
	 *            a map from a certain object that is in a property to the
	 *            variable name
	 * @param onlyPropertiesWithMatchingField
	 *            check that there is a private property for this field
	 * @param maxRecusions
	 *            the maximum of recursions that should be done
	 * @param currentRecursion
	 *            the current recursion count
	 */
	@SuppressWarnings("rawtypes")
	private String writeObject(Object o, Map<Class<?>, Integer> clazz2count,
			Map<Object, String> object2variableName,
			boolean onlyPropertiesWithMatchingField, int maxRecursions,
			int currentRecursion) {
		try {
			// if we already serialized the object
			// we just output the name of the variable
			// to create a back reference
			if (object2variableName.containsKey(o)) {
				return object2variableName.get(o);
			}

			Class<?> clazz = o.getClass();
			// write primitive types directly out
			if (isPrimitiveOrBoxClass(clazz, false)) {
				return formatType(clazz, o);
			} else if (clazz == String.class) {
				return "\"" + o.toString() + "\"";
			} else if (clazz.isEnum()) {
				return clazz.getName() + "." + ((Enum) o).name();
			}
			BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
			String beanName = getVariableName(clazz, clazz2count);
			object2variableName.put(o, beanName);
			out.write((clazz.getName() + " " + beanName + " = ").getBytes());

			if (Collection.class.isAssignableFrom(clazz)) {
				out.write(("new " + clazz.getName() + "();\n").getBytes());
				writeCollection(clazz2count, object2variableName,
						onlyPropertiesWithMatchingField, (Collection) o,
						beanName, maxRecursions, currentRecursion);
				return "";
			} else if (Map.class.isAssignableFrom(clazz)) {
				out.write(("new " + clazz.getName() + "();\n").getBytes());
				writeMap(clazz2count, object2variableName,
						onlyPropertiesWithMatchingField, (Map) o, beanName,
						maxRecursions, currentRecursion);
				return "";
			}

			// if we have no function to generate the constructors
			if (!class2constructorGenerator.containsKey(clazz)) {
				// This should throw an exception if there is no
				// no-args constructor
				try {
					clazz.getConstructor();
				} catch (NoSuchMethodException e) {
					log.log(Level.WARNING, "Exception was thrown", e);
					return "null /* Could not generate code for "
							+ clazz.getName()
							+ " there is not no args constructor */";
				}
				out.write(("new " + clazz.getName() + "()").getBytes());
			} else {
				out.write(class2constructorGenerator.get(clazz).apply(o)
						.getBytes());
				out.write((";\n".getBytes()));
				// do not recursively go down when
				// a custom constructor was supplied
				// just return the name of the variable
				return beanName;
			}
			out.write((";\n".getBytes()));
			for (PropertyDescriptor propertyDescriptor : beanInfo
					.getPropertyDescriptors()) {

				// if we should only include certain fields
				if (class2fieldIncludes.containsKey(clazz)) {
					// check if the current field should
					// be included
					if (class2fieldIncludes.get(clazz).get(
							propertyDescriptor.getName()) == null
							|| !class2fieldIncludes.get(clazz).get(
									propertyDescriptor.getName())) {
						// if not continue
						continue;
					}
				}

				if (onlyPropertiesWithMatchingField) {
					try {
						clazz.getDeclaredField(propertyDescriptor.getName());
					} catch (NoSuchFieldException e) {
						log.info("Skipping method without matching field: "
								+ propertyDescriptor.getName());
						continue;
					}
				}
				Class<?> propertyClass = propertyDescriptor.getPropertyType();
				Method readMethod = propertyDescriptor.getReadMethod();
				if (readMethod == null) {
					log.warning("Could not find read Method for: "
							+ propertyDescriptor.getName());
					continue;
				}
				Object propertyValue = readMethod.invoke(o);
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
						Method writeMethod = propertyDescriptor
								.getWriteMethod();
						if (writeMethod != null) {
							out.write((beanName + "." + writeMethod.getName()
									+ "(\"" + propertyValue + "\");\n")
									.getBytes());
						} else {
							log.warning("Can not find write method for: "
									+ o.getClass().getName() + " "
									+ propertyDescriptor.getName());
						}
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
							writeCollection(clazz2count, object2variableName,
									onlyPropertiesWithMatchingField,
									collection, collectionName, maxRecursions,
									currentRecursion);
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
							writeMap(clazz2count, object2variableName,
									onlyPropertiesWithMatchingField, map,
									mapName, maxRecursions, currentRecursion);
							out.write((beanName
									+ "."
									+ propertyDescriptor.getWriteMethod()
											.getName() + "(" + mapName + ");\n")
									.getBytes());
						} else if (propertyClass != Class.class) {

							if (maxRecursions == 0
									|| maxRecursions < currentRecursion) {
								String newBeanName = object2variableName
										.containsKey(propertyValue) ? object2variableName
										.get(propertyValue) : writeObject(
										propertyValue, clazz2count,
										object2variableName,
										onlyPropertiesWithMatchingField,
										maxRecursions, currentRecursion + 1);

								Method writeMethod = propertyDescriptor
										.getWriteMethod();
								if (writeMethod != null) {
									out.write((beanName
											+ "."
											+ propertyDescriptor
													.getWriteMethod().getName()
											+ "(" + newBeanName + ");\n")
											.getBytes());
								} else {
									log.warning("Can not find write method for: "
											+ o.getClass().getName()
											+ " "
											+ propertyDescriptor.getName());
								}
							}

						}
					}
				}
			}
			return beanName;
		} catch (IntrospectionException e) {
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
	 * Writes a map to the output stream.
	 * 
	 * @param clazz2count
	 *            how many classes were already serialized
	 * @param object2variableName
	 *            what is the name of cylic references
	 * @param onlyPropertiesWithMatchingField
	 * @param collection
	 *            the name of the collection variable
	 * @param collectionName
	 * @throws IOException
	 */
	private void writeCollection(Map<Class<?>, Integer> clazz2count,
			Map<Object, String> object2variableName,
			boolean onlyPropertiesWithMatchingField, Collection<?> collection,
			String collectionName, int maxRecursions, int currentRecursion)
			throws IOException {
		if (maxRecursions == 0 || maxRecursions < currentRecursion) {
			for (Object item : collection) {
				String itemName = writeObject(item, clazz2count,
						object2variableName, onlyPropertiesWithMatchingField,
						maxRecursions, currentRecursion + 1);
				out.write((collectionName + ".add(" + itemName + ");\n")
						.getBytes());
			}
		}
	}

	/**
	 * Writes a map to the output stream.
	 * 
	 * @param clazz2count
	 * @param object2variableName
	 * @param onlyPropertiesWithMatchingField
	 * @param map
	 * @param mapName
	 * @throws IOException
	 */
	private void writeMap(Map<Class<?>, Integer> clazz2count,
			Map<Object, String> object2variableName,
			boolean onlyPropertiesWithMatchingField, Map<?, ?> map,
			String mapName, int maxRecursions, int currentRecursion)
			throws IOException {

		if (maxRecursions == 0 || maxRecursions == currentRecursion) {
			for (Entry<?, ?> item : map.entrySet()) {
				String keyName = object2variableName.containsKey(item.getKey()) ? object2variableName
						.get(item.getKey()) : writeObject(item.getKey(),
						clazz2count, object2variableName,
						onlyPropertiesWithMatchingField, maxRecursions,
						currentRecursion + 1);
				String valueName = object2variableName.containsKey(item
						.getValue()) ? object2variableName.get(item.getValue())
						: writeObject(item.getValue(), clazz2count,
								object2variableName,
								onlyPropertiesWithMatchingField, maxRecursions,
								currentRecursion + 1);

				out.write((mapName + ".put(" + keyName + ", " + valueName + ");\n")
						.getBytes());
			}
		}
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
			return value.equals('\u0000') ? "''" : "'" + value + "'";
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

	/**
	 * Closes the output stream
	 */
	@Override
	public void close() {
		try {
			out.close();
		} catch (IOException e) {
			log.log(Level.WARNING, "Exception was thrown", e);
		}
	}

}
