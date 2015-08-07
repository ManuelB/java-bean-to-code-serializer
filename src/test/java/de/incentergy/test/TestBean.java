package de.incentergy.test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * This bean is a test for serializing
 * a bean to code to recreate the bean.
 * 
 * @author Manue Blechschmidt <manuel.blechschmidt@incentergy.de>
 *
 */
public class TestBean implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1439202533001326605L;
	
    public static enum MyEnum {
        MY_ENUM_VALUE;
    }

    /**
     * http://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
     */
    private byte myByte;
    private short myShort;
    private int myInt;
    private long myLong;
    private float myFloat;
    private double myDouble;
    private boolean myBoolean;
    private char myChar;
    private String myString;
    private MyEnum myEnum;

    // TODO: Support primitive array types
    private Collection<String> myStringCollection;
    private Map<String, TestBean> myString2TestBeanMap;
    
    private TestBean myTestBean;

    public byte getMyByte() {
        return myByte;
    }

    public void setMyByte(byte myByte) {
        this.myByte = myByte;
    }

    public short getMyShort() {
        return myShort;
    }

    public void setMyShort(short myShort) {
        this.myShort = myShort;
    }

    public int getMyInt() {
        return myInt;
    }

    public void setMyInt(int myInt) {
        this.myInt = myInt;
    }

    public long getMyLong() {
        return myLong;
    }

    public void setMyLong(long myLong) {
        this.myLong = myLong;
    }

    public float getMyFloat() {
        return myFloat;
    }

    public void setMyFloat(float myFloat) {
        this.myFloat = myFloat;
    }

    public double getMyDouble() {
        return myDouble;
    }

    public void setMyDouble(double myDouble) {
        this.myDouble = myDouble;
    }

    public boolean isMyBoolean() {
        return myBoolean;
    }

    public void setMyBoolean(boolean myBoolean) {
        this.myBoolean = myBoolean;
    }

    public char getMyChar() {
        return myChar;
    }

    public void setMyChar(char myChar) {
        this.myChar = myChar;
    }

    public String getMyString() {
        return myString;
    }

    public void setMyString(String myString) {
        this.myString = myString;
    }
	
    public MyEnum getMyEnum() {
        return myEnum;
    }

    public void setMyEnum(MyEnum myEnum) {
        this.myEnum = myEnum;
    }

    public Collection<String> getMyStringCollection() {
        return myStringCollection;
    }

    public void setMyStringCollection(Collection<String> myStringCollection) {
        this.myStringCollection = myStringCollection;
    }

    public Map<String, TestBean> getMyString2TestBeanMap() {
        return myString2TestBeanMap;
    }

    public void setMyString2TestBeanMap(
            Map<String, TestBean> myString2TestBeanMap) {
        this.myString2TestBeanMap = myString2TestBeanMap;
    }

    public TestBean getMyTestBean() {
        return myTestBean;
    }

    public void setMyTestBean(TestBean myTestBean) {
        this.myTestBean = myTestBean;
    }

}
