package com.bamboo.core.util;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import com.bamboo.core.util.ResourceConstants;
import com.bamboo.core.MessageLocalizer;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ResourceRequestHandlerUtil {

	public static Object updateResourceFromRequest(Object original, JsonObject request) {
		
		DateFormat formatter = new SimpleDateFormat(ResourceConstants.DATE_FORMAT);
		
		request.entrySet().forEach(e -> {
			try {
				if (e.getValue().getClass() == JsonPrimitive.class) {
					
					Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
					if (f.getType() == String.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), e.getValue().getAsString());
					} else if (f.getType() == int.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), new Integer(e.getValue().getAsInt()));
					} else if (f.getType() == boolean.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), new Boolean(e.getValue().getAsBoolean()));
					} else if (f.getType() == java.util.Date.class) {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), formatter.parse(e.getValue().getAsString()));
					}
					
				} else if (e.getValue().getClass() == JsonArray.class) {
					
					Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
					if (f.getType().isArray()) {
						
						if (f.getType().getComponentType() == String.class) {
							
							List<String> strs = new ArrayList<>();
							e.getValue().getAsJsonArray()
										.forEach(ele -> strs.add(ele.getAsString()));
							MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), (Object) strs.toArray(new String[strs.size()]));
						
						} else if (f.getType().getComponentType() == int.class) {
							
							List<Integer> ints = new ArrayList<>();
							e.getValue().getAsJsonArray()
										.forEach(ele -> ints.add(new Integer(ele.getAsInt())));
							MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), (Object) ints.toArray(new Integer[ints.size()]));
						
						} else if (f.getType().getComponentType() == boolean.class) {
							
							List<Boolean> booleans = new ArrayList<>();
							e.getValue().getAsJsonArray()
										.forEach(ele -> booleans.add(new Boolean(ele.getAsBoolean())));
							MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), (Object) booleans.toArray(new Boolean[booleans.size()]));
						
						} else if (f.getType().getComponentType() == java.util.Date.class) {
							
							List<Date> dates = new ArrayList<>();
							e.getValue().getAsJsonArray()
										.forEach(ele -> {
											try {
												dates.add((Date)formatter.parse(ele.getAsString()));
											} catch (ParseException ex) {
												throw new RuntimeException(ResourceConstants.INTERNAL_SERVER_ERROR, ex);
											}
										});
							MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), (Object) dates.toArray(new Date[dates.size()]));
						
						}
						
					}
					
				} else if (e.getValue().getClass() == JsonNull.class) {
					
					Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
					MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), new Object[] { null }, new Class[] { f.getType() });

				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ResourceConstants.INTERNAL_SERVER_ERROR, ex);
			}
		});

		return original;
	}

	private static String getMethodName(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "set" + key;
	}

	public static List<String> validateResource(Object resource, Validator validator, MessageLocalizer messageLocalizer, Locale locale) {
		List<String> validationMessages = new ArrayList<>();
		if (validator != null) {
			Set<ConstraintViolation<Object>> constraintViolations = validator.validate(resource);
			constraintViolations.forEach(constraint -> {
				if (messageLocalizer != null)
					validationMessages.add(messageLocalizer.getLocalizedMessage(locale, constraint.getMessage()));
				else
					validationMessages.add(constraint.getMessage());
			});
		}
		return validationMessages;
	}

}
