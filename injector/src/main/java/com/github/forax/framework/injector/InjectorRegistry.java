package com.github.forax.framework.injector;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class InjectorRegistry {

   private final HashMap<Class<?>, Supplier<?>> registry = new HashMap<>();


   static List<PropertyDescriptor> findInjectableProperties(Class<?> type) {
        Objects.requireNonNull(type);
        var beaninfo = Utils.beanInfo(type);
        /*
        Important :
        1) Filtrer pour enlever les class (propertyDescriptor.getName().equals(("class")))
        2) recupérer les setter (property.getWriteMethod();)
        3) Garder les setter annoté avec @Inject (setter.isAnnotationPresent)
        4) toList
         */
        return Arrays.stream(beaninfo.getPropertyDescriptors())
                //.filter(propertyDescriptor -> !propertyDescriptor.getName().equals(("class")))
                .filter(property -> {
                    var setter = property.getWriteMethod();
                    if(setter == null) {
                        return false;
                    }
                    return setter.isAnnotationPresent(Inject.class);
                }).toList();
   }

    // Important : Permet de vérifir que la clée et la valeur sois le même type avec la mise en place de T
    public <T> void registerInstance(Class<T> type, T instance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(instance);
        registerProvider(type, () -> instance);
    }

    public <T> T lookupInstance(Class<T> type) {
        Objects.requireNonNull(type);
        var supplier = registry.get(type);
        if(supplier == null){
            throw new IllegalStateException();
        }
        // Important 1 : type.cast au lieu de (T)
        // Important 2 : accéder au valeur du supplier pour cast
        return type.cast(supplier.get());
    }


    // Important 1 : Dans la partie Test, méthode d'instance
    // registry.registerProvider(I.class, Impl::new);
    public <T> void registerProvider(Class<T> type, Supplier<T> supplier) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        var oldValue = registry.putIfAbsent(type, supplier);
        if(oldValue != null){
            throw new IllegalStateException();
        }
    }

    private Constructor<?> findInjactableConstructor(Class<?> type) {
       var constructors = Arrays.stream(type.getConstructors())
               .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
               .toList();
       return switch (constructors.size()){
           case 0 -> Utils.defaultConstructor(type);
           case 1 -> constructors.getFirst();
           default -> throw new IllegalStateException();
       };
    }

    /*
    A documenter
     */
    public <T> void registerProviderClass(Class<T> type, Class<? extends T> implementation) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(implementation);
        var constructor = findInjactableConstructor(implementation);
        var properties = findInjectableProperties(implementation);
        registerProvider(type, () -> {
            var args = Arrays.stream(constructor.getParameterTypes())
                    .map(this::lookupInstance).toArray();
            var instance = Utils.newInstance(constructor, args);
            for(var property : properties){
                var propertyType = property.getPropertyType();
                var value = lookupInstance(propertyType);
                Utils.invokeMethod(instance, property.getWriteMethod(), value);
            }
            return implementation.cast(instance);
        });



   }
}