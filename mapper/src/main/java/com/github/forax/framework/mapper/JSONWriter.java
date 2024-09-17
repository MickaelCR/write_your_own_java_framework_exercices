package com.github.forax.framework.mapper;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class JSONWriter {

  private static final ClassValue<PropertyDescriptor[]> BEAN_INFO_CLASS_VALUE = new ClassValue<PropertyDescriptor[]>() {
    @Override
    protected PropertyDescriptor[] computeValue(Class<?> type) {
      return Utils.beanInfo(type).getPropertyDescriptors();
    }
  };

  public String toJSON(Object o) {
    return switch (o) {
      case null -> "null";
      case Boolean b -> b.toString();
      case Integer i -> i.toString();
      case Double d -> d.toString();
      case Float f -> f.toString();
      case Long l -> l.toString();
      case String s -> '"' + s + '"';
      default -> {
        var properties = BEAN_INFO_CLASS_VALUE.get(o.getClass());
        yield Arrays.stream(properties)
            .filter(property -> !property.getName().equals("class"))
            .map(property -> {
              var getter = property.getReadMethod();
              var annotation = getter.getAnnotation(JSONProperty.class);
              var name = annotation == null ? property.getName() : annotation.value();
              return '"' + name + "\": " + toJSON(Utils.invokeMethod(o, getter));
            })
                  .collect(Collectors.joining(", ", "{", "}"));
      }
    };
  }
}
