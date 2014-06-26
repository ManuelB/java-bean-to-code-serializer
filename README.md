java-bean-to-code-serializer
============================

This code takes a java bean and tries to serialize it as java code that sets recursively all the fields. This is useful if you want to mock an object and already get the object somewhere but you want to put it in the code.

This should be used together with mockito and https://github.com/ManuelB/mockito-caching-spy

To see a code example:
 * [Object2CodeObjectOutputStreamTest.java](https://github.com/ManuelB/java-bean-to-code-serializer/blob/master/src/test/java/de/apaxo/test/Object2CodeObjectOutputStreamTest.java)

To use it add the following maven dependency:
```
  <dependency>
    <groupId>de.apaxo.test</groupId>
    <artifactId>java-bean-to-code-serializer</artifactId>
    <version>0.7</version>
  </dependency>
```

This code is used in the [Apaxo GmbH Platform](http://www.apaxo.de)
