package com.bamboo.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.glassfish.hk2.runlevel.RunLevelException;

import com.bamboo.core.util.ResourceConstants;
import com.bamboo.core.MessageLocalizer;
import com.bamboo.core.Resource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ResourceRequestHandlerUtil {

	public static Map<String, Map<String, List<Object>>> updateResourceFromRequest(Resource original, String request) {
		
		JsonObject putObject = new JsonParser().parse(request).getAsJsonObject();
		JsonArray operations = putObject.getAsJsonArray(ResourceConstants.OPERATIONS_TAG); 
		Map<String, Map<String, List<Object>>> operationsMap = new HashMap<>();
		
		operations.forEach(operation -> {
			String property = operation.getAsJsonObject().get("property").getAsString();
			
			String op = operation.getAsJsonObject().get("op").getAsString();
			if(!(op.equals("add") || op.equals("remove"))){
				throw new RuntimeException("Invalid operation type");
			}
			Object value = operation.getAsJsonObject().get("value");
			
			if(operationsMap.get(property) == null){
				List<Object> valueList = new ArrayList<>();
				valueList.add(value);
				Map<String, List<Object>> valueOpMap = new HashMap<>();
				valueOpMap.put(op, valueList);
				operationsMap.put(property,valueOpMap);
			}else{
				Map<String, List<Object>> valueOpMap = operationsMap.get(property);
				if(valueOpMap.get(op) != null){
					List<Object> valueList = valueOpMap.get(op);
					valueList.add(value);
				}else{
					List<Object> valueList = new ArrayList<>();
					valueList.add(value);
					valueOpMap.put(op, valueList);
				}
			}
		
		});
		
		
		DateFormat formatter = new SimpleDateFormat(ResourceConstants.DATE_FORMAT);
	
		//First only parse simple properties
		putObject.get(ResourceConstants.POST_ROOT_TAG).getAsJsonObject().entrySet().forEach(e -> {
			try {
				if (!(e.getValue().getClass() == JsonNull.class)) {
					
					Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
					
					if (f.getType() == String.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), e.getValue().getAsString());
					} else if (f.getType() == Number.class) {
						Object value = f.getType().getConstructor(String.class).newInstance(e.getValue().getAsString());
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), value);
					} else if (f.getType() == boolean.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), e.getValue().getAsBoolean());
					} else if (f.getType() == java.util.Date.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), formatter.parse(e.getValue().getAsString()));
					} else if(f.getType() == BigDecimal.class){
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), e.getValue().getAsBigDecimal());
					} else if(Arrays.asList(f.getType().getInterfaces()).contains(Resource.class)){
						Resource nestedResource = (Resource)f.getType().newInstance();
						MethodUtils.invokeExactMethod(nestedResource, getMethodName("id"), e.getValue().getAsString());
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), nestedResource);
					}
				}else{
					Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
					MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), new Object[] { null }, new Class[] { f.getType() });
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ResourceConstants.INTERNAL_SERVER_ERROR, ex);
			}
		});
		
		for(Entry<String, Map<String, List<Object>>> entry : operationsMap.entrySet()){
			try {
				Field f = FieldUtils.getDeclaredField(original.getClass(), entry.getKey(), true);
				if(!f.getType().isArray())
					throw new Exception("Invalid request - 400");
				
				Object[] propertyArray = (Object[]) MethodUtils.invokeExactMethod(original, formMethodNameForGet(entry.getKey()), new Object[]{});
				List<Object> propertyValueList = new ArrayList();
				propertyValueList.addAll(Arrays.asList(propertyArray));
				
				for(Entry<String, List<Object>> opValueEntry : entry.getValue().entrySet()){
					for(Object value : opValueEntry.getValue()){
						Object propValue = null;
						if (f.getType().getComponentType() == String.class) {
							propValue = ((JsonElement)value).getAsString();
						} else if (f.getType().getComponentType() == BigDecimal.class) {
							propValue = ((JsonElement)value).getAsBigDecimal();
						} else if (f.getType().getComponentType() == boolean.class) {
							propValue = ((JsonElement)value).getAsBoolean();
						} else if (f.getType().getComponentType() == java.util.Date.class) {
							propValue = formatter.parse(((JsonElement)value).getAsString());
						} else if(Arrays.asList(f.getType().getComponentType().getInterfaces()).contains(Resource.class)){
							//This is array of nested resources
							String id = ((JsonElement)value).getAsString();
							if(opValueEntry.getKey().equals("add")){
								Resource res = (Resource) f.getType().getComponentType().newInstance();
								res.setId(id);
								propertyValueList.add(res);
							}else{
								propertyValueList = propertyValueList.stream().filter(obj -> !((Resource)obj).getId().equals(id)).collect(Collectors.toList());
							}
							continue;
						}
						
						if(opValueEntry.getKey().equals("add")){
							propertyValueList.add(propValue);
						}else{
							propertyValueList.remove(propValue);
						}
					}
				}
				
				//Set the array back in resource
				MethodUtils.invokeExactMethod(original, getMethodName(f.getName()), (Object) propertyValueList.toArray((Object[]) Array.newInstance(f.getType().getComponentType(), propertyValueList.size())));
				
			}catch(Exception ex){
				throw new RuntimeException(ex);
			}
		}
		
		return operationsMap;
	}

	private static String getMethodName(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "set" + key;
	}

	public static List<String> validateResource(Resource resource, Validator validator, boolean validateNested, List<String> messages, MessageLocalizer messageLocalizer, Locale locale) {
		try{
			if (validator != null) {
				Set<ConstraintViolation<Object>> constraintViolations = validator.validate(resource);
				for(ConstraintViolation<Object> constraint :constraintViolations){
					if (messageLocalizer != null)
						messages.add(messageLocalizer.getLocalizedMessage(locale, constraint.getMessage()));
					else
						messages.add(constraint.getMessage());
				}
				
				if(!validateNested)
					return messages;
				
				//Check for nested fields and validate each of them
				Field[] fields = resource.getResourceClass().getDeclaredFields();
				for(Field field : fields){
					field.setAccessible(true);
					if(field.getType() == Resource.class && (Resource) MethodUtils.invokeExactMethod(resource, formMethodNameForGet(field.getName())) != null){
						messages = validateResource((Resource) MethodUtils.invokeExactMethod(resource, formMethodNameForGet(field.getName())), validator, validateNested, messages, messageLocalizer, locale);
					}
					
					if(field.getType().isArray() && (field.getType().getComponentType().getInterfaces() != null && Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))){
						Resource[] arrayFieldValues = (Resource[]) MethodUtils.invokeExactMethod(resource, formMethodNameForGet(field.getName()));
						if(arrayFieldValues != null && arrayFieldValues.length > 0){
							for(Resource nestedResource : arrayFieldValues){
								if(nestedResource != null)
									messages = validateResource(nestedResource, validator, validateNested, messages, messageLocalizer, locale);
							}
						}
					}
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return messages;
	}
	
	public static Object setLinkedObjectsInRoot(Object rootObject, Map<Resource, Object> linkedObjects){
		linkedObjects.entrySet().forEach(entry ->{
			try {
				MethodUtils.invokeExactMethod(rootObject, getMethodName(entry.getKey().getResourceName()), new Object[] { entry.getValue() }, new Class[] { entry.getKey().getResourceClass()});
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ResourceConstants.INTERNAL_SERVER_ERROR, ex);
			}
		});
		return rootObject;
	}

	private static String formMethodNameForGet(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "get" + key;
	}
	
}
