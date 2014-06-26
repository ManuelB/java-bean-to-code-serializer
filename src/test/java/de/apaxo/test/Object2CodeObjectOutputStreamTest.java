package de.apaxo.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Object2CodeObjectOutputStreamTest {

    private static final Logger log = Logger
            .getLogger(Object2CodeObjectOutputStreamTest.class.getName());

    @Test
    public void testWritePrimitive() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
                byteArrayOutputStream)) {
            object2CodeObjectOutputStream.writeObject(5);
            assertEquals("5",
                    byteArrayOutputStream.toString());
        } catch (SecurityException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        }
    }
    
    @Test
    public void testNull() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (Object2CodeObjectOutputStream object2CodeObjectOutputStream = new Object2CodeObjectOutputStream(
                byteArrayOutputStream)) {
            object2CodeObjectOutputStream.writeObject(null);
            assertEquals("",
                    byteArrayOutputStream.toString());
        } catch (SecurityException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
        }
    }

    @Test
    public void testWriteObject() {
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

            Collection<String> strings = new ArrayList<String>(Arrays.asList(
                    "one", "two", "three"));
            testBean0.setMyStringCollection(strings);

            TestBean testBean1 = new TestBean();
            Map<String, TestBean> map = new HashMap<String, TestBean>();
            map.put("testBean0", testBean0);
            map.put("testBean1", testBean1);
            testBean0.setMyString2TestBeanMap(map);
            testBean0.setMyTestBean(testBean1);

            object2CodeObjectOutputStream.writeObject(testBean0);
            String code = byteArrayOutputStream.toString();
            log.info(code);
            assertEquals(
                    "de.apaxo.test.TestBean testBean0 = new de.apaxo.test.TestBean();\n"
                            + "testBean0.setMyBoolean(true);\n"
                            + "testBean0.setMyByte((byte) 82);\n"
                            + "testBean0.setMyChar('k');\n"
                            + "testBean0.setMyDouble(2.3);\n"
                            + "testBean0.setMyEnum(de.apaxo.test.TestBean$MyEnum.MY_ENUM_VALUE);\n"
                            + "testBean0.setMyFloat(2.3f);\n"
                            + "testBean0.setMyInt(255);\n"
                            + "testBean0.setMyLong(656l);\n"
                            + "testBean0.setMyShort((short)45);\n"
                            + "testBean0.setMyString(\"String\");\n"
                            + "java.util.HashMap hashMap0 = new java.util.HashMap();\n"
                            + "hashMap0.put(\"testBean0\", testBean0);\n"
                            + "de.apaxo.test.TestBean testBean1 = new de.apaxo.test.TestBean();\n"
                            + "testBean1.setMyBoolean(false);\n"
                            + "testBean1.setMyByte((byte) 0);\n"
                            + "testBean1.setMyChar('\0');\n"
                            + "testBean1.setMyDouble(0.0);\n"
                            + "testBean1.setMyFloat(0.0f);\n"
                            + "testBean1.setMyInt(0);\n"
                            + "testBean1.setMyLong(0l);\n"
                            + "testBean1.setMyShort((short)0);\n"
                            + "hashMap0.put(\"testBean1\", testBean1);\n"
                            + "testBean0.setMyString2TestBeanMap(hashMap0);\n"
                            + "java.util.ArrayList arrayList0 = new java.util.ArrayList();\n"
                            + "arrayList0.add(\"one\");\n"
                            + "arrayList0.add(\"two\");\n"
                            + "arrayList0.add(\"three\");\n"
                            + "testBean0.setMyStringCollection(arrayList0);\n"
                            + "testBean0.setMyTestBean(testBean1);\n", code);
        } catch (SecurityException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
            fail();
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
            fail();
        }

    }
}
