package com.bamboo.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class RequestResourceHandlerUtil {

	public static Object updateFromPUTRequest(Object original, JsonObject request) {

		request.entrySet().forEach(e -> {
			if (e.getValue().getClass() == JsonPrimitive.class) {
				Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
				if (f.getType() == String.class) {
					try {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), e.getValue().getAsString());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else if (f.getType() == int.class) {
					try {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()),
								new Integer(e.getValue().getAsInt()));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else if (f.getType() == boolean.class) {
					try {
						MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()),
								new Boolean(e.getValue().getAsBoolean()));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else if (f.getType() == java.util.Date.class) {
					// to-do
				}
			} else if (e.getValue().getClass() == JsonArray.class) {
				Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
				if (f.getType().isArray()) {
					if (f.getType().getComponentType() == String.class) {
						List<String> strs = new ArrayList<>();
						e.getValue().getAsJsonArray().forEach(ele -> strs.add(ele.getAsString()));
						try {
							MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()),
									(Object) strs.toArray(new String[strs.size()]));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			} else if(e.getValue().getClass() == JsonNull.class){
				Field f = FieldUtils.getDeclaredField(original.getClass(), e.getKey(), true);
				try {
					MethodUtils.invokeExactMethod(original, getMethodName(e.getKey()), new Object[]{null}, new Class[]{f.getType()});
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
			}
		});

		return original;
	}

	private static String getMethodName(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "set" + key;
	}
}
