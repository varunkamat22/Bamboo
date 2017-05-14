package com.bamboo.core.types;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.reflect.MethodUtils;
import com.bamboo.core.Resource;

public class ResourceWrapper {
	
	private Resource resource;
	private List<Field> simpleFields = new ArrayList<>();
	private List<Field> simpleArrayFields = new ArrayList<>();
	private List<Field> nestedResourceFields =  new ArrayList<>();
	private List<Field> nestedResourceArrayFields =  new ArrayList<>();
	

	public ResourceWrapper(Resource resource) {
		this.resource = resource;
		init();
	}
	
	private void init(){
		for(Field field : resource.getClass().getDeclaredFields()){
			if(field.getType().isArray()){
				if(Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class)){
					nestedResourceArrayFields.add(field);
				}else{
					simpleArrayFields.add(field);
				}
			}else{
				if(field.getType().getInterfaces() != null && Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)){
					nestedResourceFields.add(field);
				}else{
					simpleFields.add(field);
				}
			}
		}
	}
	
	public void setFieldValue(String fieldName, Object fieldValue){
		try{
			Field field = resource.getClass().getDeclaredField(fieldName);
			
			if(isSimpleArray(fieldName)){ 
				List<Object> nestedObjects = (List<Object>) fieldValue;
				MethodUtils.invokeExactMethod(resource, setterMethodName(fieldName),
						                      (Object)nestedObjects.toArray((Object[]) Array.newInstance(getComponentType(fieldName), nestedObjects.size())));
			}else if(isResourceArray(fieldName)){
				List<Object> nestedObjects = (List<Object>) fieldValue;
				MethodUtils.invokeExactMethod(resource, setterMethodName(fieldName),
						                      (Object)nestedObjects.toArray((Object[]) Array.newInstance(getComponentType(fieldName), nestedObjects.size())));
			}else if(isResourceType(fieldName)){
				MethodUtils.invokeExactMethod(resource, setterMethodName(fieldName), fieldValue);
			}else{
				MethodUtils.invokeExactMethod(resource, setterMethodName(fieldName), fieldValue);
			}
		}catch(NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException  ex){
			throw new RuntimeException(ex);
		}
	}
	
	public Object getFieldValue(String fieldName){
		try{
			return MethodUtils.invokeExactMethod(resource, getterMethodName(fieldName), new Object[]{}); 
		}catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException  ex){
			throw new RuntimeException(ex);
		}
	}
	
	public Resource getResource() {
		return resource;
	}

	public List<Field> getSimpleFields() {
		return simpleFields;
	}

	public List<Field> getSimpleArrayFields() {
		return simpleArrayFields;
	}

	public List<Field> getNestedResourceFields() {
		return nestedResourceFields;
	}

	public List<Field> getNestedResourceArrayFields() {
		return nestedResourceArrayFields;
	}

	public boolean isResourceType(String fieldName){
		try{
			if(resource.getClass().getDeclaredField(fieldName).getType().getInterfaces() != null)
				return Arrays.asList(resource.getClass().getDeclaredField(fieldName).getType().getInterfaces()).contains(Resource.class);
			else
				return false;
		}catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}
	}
	
	public boolean isSimpleArray(String fieldName){
		try{
			return resource.getClass().getDeclaredField(fieldName).getType().isArray() &&
				   !Arrays.asList(resource.getClass().getDeclaredField(fieldName).getType().getComponentType().getInterfaces()).contains(Resource.class);
		}catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}
	}
	
	public boolean isResourceArray(String fieldName){
		try{
			return resource.getClass().getDeclaredField(fieldName).getType().isArray() &&
				   Arrays.asList(resource.getClass().getDeclaredField(fieldName).getType().getComponentType().getInterfaces()).contains(Resource.class);
		}catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}
	}
	
	public Class<?> getComponentType(String fieldName){
		try{
			return resource.getClass().getDeclaredField(fieldName).getType().getComponentType();
		}catch(NoSuchFieldException nsfe){
			throw new RuntimeException(nsfe);
		}
	}
	
	public Resource getComponentInstance(Field field, int id){
		try{
			Resource nestedResource = null;
			if(isResourceArray(field.getName()))
				nestedResource = (Resource) field.getType().getComponentType().newInstance();
			else
				nestedResource = (Resource) field.getType().newInstance();
			MethodUtils.invokeExactMethod(nestedResource, "setId", String.valueOf(id));
			return nestedResource;
		}catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException  ex){
			throw new RuntimeException(ex);
		}
	}
	
	private String getterMethodName(String fieldName){
		return "get"+fieldName.toUpperCase().charAt(0)+fieldName.substring(1);
	}
	
	private String setterMethodName(String fieldName){
		return "set"+fieldName.toUpperCase().charAt(0)+fieldName.substring(1);
	}

}
