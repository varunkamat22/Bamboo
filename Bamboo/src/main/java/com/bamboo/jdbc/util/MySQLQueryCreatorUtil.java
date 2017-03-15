package com.bamboo.jdbc.util;

import java.lang.reflect.Field;
import com.bamboo.core.SearchCriteria;
import com.bamboo.jdbc.SearchQueryBuilder;

public class MySQLQueryCreatorUtil {
	
	public static String formRetrieveAllQuery(String resourceName){
		return String.format("SELECT * FROM %s", resourceName.toUpperCase());
	}
	
	public static String formRetrieveByIDQuery(String id, String resourceName){
		return String.format("SELECT * FROM %s where id = %s", resourceName.toUpperCase(), id);
	}
	
	public static String formInsertQuery(String resourceName, Class resourceType){
		Field[] fields = resourceType.getDeclaredFields();
		StringBuilder queryBuilder = new StringBuilder();
		String part1 = String.format("INSERT INTO %s (", resourceName.toUpperCase());
		queryBuilder.append(part1);
		for(Field field : fields){
			if(field.getName().equals("id"))
				continue;
			queryBuilder.append(String.format("%s, ",field.getName()));
		}
		queryBuilder.append(") VALUES(");
		for(Field field : fields){
			if(field.getName().equals("id"))
				continue;
			queryBuilder.append(String.format(":%s, ",field.getName()));
		}
		queryBuilder.append(")");
		return queryBuilder.toString().replaceAll(", \\)", "\\)");
	}
	
	public static String formDeleteQuery(String id, String resourceName){
		return String.format("DELETE FROM %s where id = %s", resourceName.toUpperCase(), id);
	}
	
	public static String formRetriveWithFilterQuery(String resourceName, SearchCriteria criteria, SearchCriteria sortCriteria, int batchSize, int startIndex, SearchQueryBuilder searchQueryBuilder){
		String searchQuery =  String.format("SELECT * FROM %s where %s", resourceName.toUpperCase(), searchQueryBuilder.formSearchQuery(criteria));
		if(sortCriteria != null)
			searchQuery =  searchQuery.concat(String.format(" ORDER BY %s %s", sortCriteria.getLeftOperand(), sortCriteria.getRightOperand().toString().toUpperCase()));
		if(batchSize != 0){
			startIndex = startIndex > 0 ? startIndex : 1;
			searchQuery = searchQuery.concat(String.format(" LIMIT %s,%s", (startIndex-1)*batchSize, ((startIndex-1)*batchSize)+batchSize));
		}
		return searchQuery;
	}
	
	public static String formUpdateQuery(String resourceName, Class resourceType){
		String part1 = String.format("UPDATE %s SET ", resourceName.toUpperCase());
		StringBuilder queryBuilder = new StringBuilder(part1);
		Field[] fields = resourceType.getDeclaredFields();
		for(Field field : fields){
			if(field.getName().equals("id"))
				continue;
			queryBuilder.append(String.format("%s=:%s, ",field.getName(), field.getName()));
		}
		queryBuilder.append("WHERE id=:id");
		return  queryBuilder.toString().replaceFirst(", WHERE", " WHERE");		
	}
	
}
