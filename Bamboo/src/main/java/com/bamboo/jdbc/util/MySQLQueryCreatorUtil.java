package com.bamboo.jdbc.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.bamboo.core.Resource;
import com.bamboo.core.SearchCriteria;
import com.bamboo.jdbc.SearchQueryBuilder;

public class MySQLQueryCreatorUtil {
	
	public static String formRetrieveAllQuery(Resource resource){
		//return String.format("SELECT * FROM %s", resourceName.toUpperCase());
		Field[] fields = resource.getResourceClass().getDeclaredFields();
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ");
		for(Field field : fields){
			if(field.getType().isArray() || (field.getType().getInterfaces() != null && Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)))
				continue;
			queryBuilder.append(field.getName()+",");
		}
		queryBuilder.setCharAt(queryBuilder.length()-1, ' ');
		queryBuilder.append("FROM %s");
		return String.format(queryBuilder.toString(), resource.getResourceName().toUpperCase());
	}
	
	public static String formRetrieveByIDQuery(String id, Resource resource){
		Field[] fields = resource.getResourceClass().getDeclaredFields();
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ");
		for(Field field : fields){
			if(field.getType().isArray() || (field.getType().getInterfaces() != null && Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)))
				continue;
			queryBuilder.append(field.getName()+",");
		}
		queryBuilder.setCharAt(queryBuilder.length()-1, ' ');
		queryBuilder.append("FROM %s where id = %s");
		return String.format(queryBuilder.toString(), resource.getResourceName().toUpperCase(), id);
	}
	
	public static String formRetrieveSimpleArrayQuery(Resource resource, Field field){
		String s =  String.format("SELECT %s FROM %s where %s=%s",field.getName().toLowerCase(), resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_DATA", resource.getResourceName().toLowerCase(), resource.getId());
		return s;
	}
	
	public static String formRetrieveNestedArrayQuery(Resource resource, Field field){
		String s =  String.format("SELECT %s FROM %s where %s=%s", field.getName().toLowerCase(), resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_INFO", resource.getResourceName().toLowerCase(), resource.getId());
		return s;
	}
	
	public static String formRetrieveNestedObject(Resource resource, Field field){
		String s =  String.format("SELECT %s FROM %s where %s=%s", field.getName().toLowerCase(), resource.getResourceName().toUpperCase(), "id", resource.getId());
		return s;
	}
	
	public static String formInsertQuery(String resourceName, Class resourceType){
		Field[] fields = resourceType.getDeclaredFields();
		StringBuilder queryBuilder = new StringBuilder();
		String part1 = String.format("INSERT INTO %s (", resourceName.toUpperCase());
		queryBuilder.append(part1);
		for(Field field : fields){
			if(field.getName().equals("id"))
				continue;
			if(field.getType().isArray())
				continue;
			queryBuilder.append(String.format("%s, ",field.getName()));
		}
		queryBuilder.append(") VALUES(");
		for(Field field : fields){
			if(field.getName().equals("id"))
				continue;
			if(field.getType().isArray())
				continue;
			queryBuilder.append(String.format(":%s, ",field.getName()));
		}
		queryBuilder.append(")");
		return queryBuilder.toString().replaceAll(", \\)", "\\)");
	}
	
	public static String formInsertQueryForLinkedTable(){
		return "INSERT INTO %s (%s,%s) VALUES(%s,%s)";
	}
	
	
	public static String formDeleteQuery(){
		return "DELETE FROM %s where %s=%s";
	}
	
	public static String formUpdateQueryForRemoveArrayValue(){
		return "DELETE FROM %s where %s = %s and %s = %s";
	}
	
	public static String formRetriveWithFilterQuery(Resource resource, SearchCriteria criteria, SearchCriteria sortCriteria, int batchSize, int startIndex, SearchQueryBuilder searchQueryBuilder){
		//String searchQuery =  String.format("SELECT * FROM %s where %s", resource.getResourceName().toUpperCase(), searchQueryBuilder.formSearchQuery(criteria));
		
		String searchQuery = null;
		
		Field[] fields = resource.getResourceClass().getDeclaredFields();
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT ");
		for(Field field : fields){
			if(field.getType().isArray() || (field.getType().getInterfaces() != null && Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)))
				continue;
			queryBuilder.append(field.getName()+",");
		}
		queryBuilder.setCharAt(queryBuilder.length()-1, ' ');
		queryBuilder.append("FROM %s WHERE %s");
		
		searchQuery = String.format(queryBuilder.toString(), resource.getResourceName().toUpperCase(), searchQueryBuilder.formSearchQuery(criteria));
		
		if(sortCriteria != null)
			searchQuery =  searchQuery.concat(String.format(" ORDER BY %s %s", sortCriteria.getLeftOperand(), sortCriteria.getRightOperand().toString().toUpperCase()));
		if(batchSize != 0){
			startIndex = startIndex > 0 ? startIndex : 1;
			searchQuery = searchQuery.concat(String.format(" LIMIT %s,%s", (startIndex-1)*batchSize, ((startIndex-1)*batchSize)+batchSize));
		}
		return searchQuery;
	}
	
	public static String formUpdateQuery(Resource resource){
		StringBuilder queryBuilder = null;
		
		try{
			String part1 = String.format("UPDATE %s SET ", resource.getResourceName().toUpperCase());
			queryBuilder = new StringBuilder(part1);
			Field[] fields = resource.getClass().getDeclaredFields();
			for(Field field : fields){
				if(field.getName().equals("id"))
					continue;
				if(field.getType().isArray()){
					
					continue;
				}
				if(Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)){
					Resource nestedResource = (Resource)MethodUtils.invokeExactMethod(resource, getMethodName(field.getName()), new Object[]{});
					if(nestedResource != null){
						queryBuilder.append(String.format("%s=%s, ",field.getName(), nestedResource.getId()));
					}else{
						queryBuilder.append(String.format("%s=%s, ",field.getName(), "null"));
					}
					continue;
				}
				queryBuilder.append(String.format("%s=:%s, ",field.getName(), field.getName()));
			}
			queryBuilder.append("WHERE id=:id");
		
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		
		return  queryBuilder.toString().replaceFirst(", WHERE", " WHERE");		
	}
	
	public static String formClearArrayQuery(){
		return "DELETE from %s where %s=%s";
	}
	
	
	private static String getMethodName(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "get" + key;
	}
	
}
