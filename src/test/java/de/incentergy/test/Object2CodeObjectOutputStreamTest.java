package de.incentergy.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.incentergy.test.TestBean.MyInnerClass;
import org.junit.Test;

public class Object2CodeObjectOutputStreamTest {

	private static final Logger log = Logger
			.getLogger(Object2CodeObjectOutputStreamTest.class.getName());

	@Test
	public void testWritePrimitive() throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {
			object2CodeObjectOutputStream.writeObject(5);
			assertEquals("5", byteArrayOutputStream.toString());
		}
	}

	@Test
	public void testWriteList() throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {
			List<Integer> arrayList = new ArrayList<>();
			arrayList.add(1);
			arrayList.add(2);
			arrayList.add(3);
			object2CodeObjectOutputStream.writeObject(arrayList);
			assertEquals("java.util.ArrayList arrayList0 = new java.util.ArrayList();\n"
					+ "arrayList0.add(1);\n" + "arrayList0.add(2);\n"
					+ "arrayList0.add(3);\n" + "",
					byteArrayOutputStream.toString());
		}
	}

	@Test
	public void testNull() throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {
			object2CodeObjectOutputStream.writeObject(null);
			assertEquals("", byteArrayOutputStream.toString());
		}
	}

	@Test
	public void testWriteObject() throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {

			TestBean testBean0 = new TestBean();
			testBean0.setMyByte((byte) 0x52);
			testBean0.setMyShort((short) 45);
			testBean0.setMyInt(255);
			testBean0.setMyLong(656l);
			testBean0.setMyFloat(2.3f);
			testBean0.setMyDouble(2.3);
			testBean0.setMyBoolean(true);
			testBean0.setMyChar('k');
			testBean0.setMyString("String");
			testBean0.setMyEnum(TestBean.MyEnum.MY_ENUM_VALUE);
			MyInnerClass myInnerClass0 = new MyInnerClass();
			myInnerClass0.setValue("Inner String");
			testBean0.setMyInnerClass(myInnerClass0);

			Collection<String> strings = new ArrayList<String>(Arrays.asList(
					"one", "two", "three"));
			testBean0.setMyStringCollection(strings);

			TestBean testBean1 = new TestBean();
			// Java 1.8 has another sorting than java 1.7
			// so use a linkes hash map to be compatible
			Map<String, TestBean> map = new LinkedHashMap<String, TestBean>();
			map.put("testBean0", testBean0);
			map.put("testBean1", testBean1);
			testBean0.setMyString2TestBeanMap(map);
			testBean0.setMyTestBean(testBean1);

			object2CodeObjectOutputStream.writeObject(testBean0);
			String code = byteArrayOutputStream.toString();
			// log.info(code);
			assertEquals(
					"de.incentergy.test.TestBean testBean0 = new de.incentergy.test.TestBean();\n"
							+ "testBean0.setMyBoolean(true);\n"
							+ "testBean0.setMyByte((byte) 82);\n"
							+ "testBean0.setMyChar('k');\n"
							+ "testBean0.setMyDouble(2.3);\n"
							+ "testBean0.setMyEnum(de.incentergy.test.TestBean.MyEnum.MY_ENUM_VALUE);\n"
							+ "testBean0.setMyFloat(2.3f);\n"
							+ "de.incentergy.test.TestBean.MyInnerClass myInnerClass0 ="
							+ " new de.incentergy.test.TestBean.MyInnerClass();\n"
							+ "myInnerClass0.setValue(\"Inner String\");\n"
							+ "testBean0.setMyInnerClass(myInnerClass0);\n"
							+ "testBean0.setMyInt(255);\n"
							+ "testBean0.setMyLong(656l);\n"
							+ "testBean0.setMyShort((short)45);\n"
							+ "testBean0.setMyString(\"String\");\n"
							+ "java.util.LinkedHashMap linkedHashMap0 = new java.util.LinkedHashMap();\n"
							+ "linkedHashMap0.put(\"testBean0\", testBean0);\n"
							+ "de.incentergy.test.TestBean testBean1 = new de.incentergy.test.TestBean();\n"
							+ "testBean1.setMyBoolean(false);\n"
							+ "testBean1.setMyByte((byte) 0);\n"
							+ "testBean1.setMyChar('');\n"
							+ "testBean1.setMyDouble(0.0);\n"
							+ "testBean1.setMyFloat(0.0f);\n"
							+ "testBean1.setMyInt(0);\n"
							+ "testBean1.setMyLong(0l);\n"
							+ "testBean1.setMyShort((short)0);\n"
							+ "linkedHashMap0.put(\"testBean1\", testBean1);\n"
							+ "testBean0.setMyString2TestBeanMap(linkedHashMap0);\n"
							+ "java.util.ArrayList arrayList0 = new java.util.ArrayList();\n"
							+ "arrayList0.add(\"one\");\n"
							+ "arrayList0.add(\"two\");\n"
							+ "arrayList0.add(\"three\");\n"
							+ "testBean0.setMyStringCollection(arrayList0);\n"
							+ "testBean0.setMyTestBean(testBean1);\n", code);
		}

	}

	@Test
	public void testWriteObjectWithCustomConstructor()
			throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {

			Object2CodeObjectOutputStream.addCustomConstructorGenerator(
					GregorianCalendar.class, (o) -> {
						return "Calendar.getInstance()";
					});

			TestBeanWithCustomClassConstructor testBean0 = new TestBeanWithCustomClassConstructor();

			object2CodeObjectOutputStream.writeObject(testBean0);
			String code = byteArrayOutputStream.toString();
			// log.info(code);
			assertEquals("de.incentergy.test.TestBeanWithCustomClassConstructor testBeanWithCustomClassConstructor0 = new de.incentergy.test.TestBeanWithCustomClassConstructor();\n" + 
					"java.util.GregorianCalendar gregorianCalendar0 = Calendar.getInstance();\n" + 
					"testBeanWithCustomClassConstructor0.setCalendar(gregorianCalendar0);\n" + 
					"", code);
		}
	}
	
	@Test
	public void testWriteObjectWithProcessor()
			throws SecurityException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
				byteArrayOutputStream)) {

			// Add a processor that makes a string from every Integer
			Object2CodeObjectOutputStream.addProcessor((o) -> o instanceof Integer ? o.toString() : o);

			Integer i = new Integer(1);

			object2CodeObjectOutputStream.writeObject(i);
			String code = byteArrayOutputStream.toString();

			assertEquals("\"1\"", code);
			
			Object2CodeObjectOutputStream.clearProcessor();
			byteArrayOutputStream.reset();
			object2CodeObjectOutputStream.writeObject(i);
			
			code = byteArrayOutputStream.toString();
			// log.info(code);
			assertEquals("1", code);
		}
	}
}
