package com.bamboo.jdbc;

import java.lang.reflect.Array;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.glassfish.hk2.runlevel.RunLevelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bamboo.core.Resource;
import com.bamboo.core.SearchCriteria;
import com.bamboo.jdbc.util.MySQLQueryCreatorUtil;
import com.google.gson.JsonElement;

@Repository("mySQLPersistanceHelperImpl")
public class MySQLPersistanceHelperImpl implements PersistanceHelper {

	@Autowired(required = true)
	private DataSource dataSource;

	@Autowired(required = true)
	private SearchQueryBuilder searchQueryBuilder;

	@Override
	public List<Resource> retrieveAll(Resource resource) {
		List<Resource> results;

		try {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			results = template.query(MySQLQueryCreatorUtil.formRetrieveAllQuery(resource), new BeanPropertyRowMapper(resource.getClass()));

			for (Resource result : results) {
				
				for (Field field : resource.getResourceClass().getDeclaredFields()) {

					if (field.getType().isArray() && (field.getType().getComponentType().getInterfaces() == null
							|| !Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
						// This is an array of simple type - fetch and add to resource
						List arrayValues = template.queryForList(
								MySQLQueryCreatorUtil.formRetrieveSimpleArrayQuery(result, field),
								field.getType().getComponentType());
						field.setAccessible(true);
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
								(Object) arrayValues.toArray((Object[]) Array.newInstance(field.getType().getComponentType(), arrayValues.size())));
					} else if (field.getType().isArray() && (field.getType().getComponentType().getInterfaces() != null
							&& Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
						// This is an array of nested type - fetch and add nested type
						List<Integer> arrayValues = template.queryForList(
								MySQLQueryCreatorUtil.formRetrieveNestedArrayQuery(result, field), int.class);
						List<Object> nestedObjects = new ArrayList<>();

						for (int nestedId : arrayValues) {
							Object ob = field.getType().getComponentType().newInstance();
							MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(nestedId));
							nestedObjects.add(ob);
						}

						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
								(Object) nestedObjects.toArray((Object[]) Array
										.newInstance(field.getType().getComponentType(), nestedObjects.size())));
					} else if (!field.getType().isArray() && (field.getType().getInterfaces() != null
							&& Arrays.asList(field.getType().getInterfaces()).contains(Resource.class))) {
						// This is a single valued nested resource
						int objectId = template.queryForObject(
								MySQLQueryCreatorUtil.formRetrieveNestedObject(result, field), int.class);
						Object ob = field.getType().newInstance();
						MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(objectId));
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()), ob);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return results;
	}

	@Override
	public Resource retrieveByID(String id, Resource resource) {
		Resource result = null;
		try {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			List<Object> results = template.query(MySQLQueryCreatorUtil.formRetrieveByIDQuery(id, resource),
					new BeanPropertyRowMapper(resource.getResourceClass()));
			result = results.isEmpty() ? null : (Resource) results.get(0);
			
			if(result == null)
				return null;
			
			for (Field field : resource.getResourceClass().getDeclaredFields()) {

				if (field.getType().isArray() && (field.getType().getComponentType().getInterfaces() == null || !Arrays
						.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
					// This is an array of simple type - fetch and add to
					// resource
					List arrayValues = template.queryForList(
							MySQLQueryCreatorUtil.formRetrieveSimpleArrayQuery(result, field),
							field.getType().getComponentType());
					field.setAccessible(true);
					MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
							(Object) arrayValues.toArray((Object[]) Array.newInstance(field.getType().getComponentType(), arrayValues.size())));
				} else if (field.getType().isArray()
						&& (field.getType().getComponentType().getInterfaces() != null && Arrays
								.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
					// This is an array of nested type - fetch and add nested
					// type
					List<Integer> arrayValues = template
							.queryForList(MySQLQueryCreatorUtil.formRetrieveNestedArrayQuery(result, field), int.class);
					List<Object> nestedObjects = new ArrayList<>();

					for (int nestedId : arrayValues) {
						Object ob = field.getType().getComponentType().newInstance();
						MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(nestedId));
						nestedObjects.add(ob);
					}

					MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
							(Object) nestedObjects.toArray((Object[]) Array
									.newInstance(field.getType().getComponentType(), nestedObjects.size())));
				} else if (!field.getType().isArray() && (field.getType().getInterfaces() != null
						&& Arrays.asList(field.getType().getInterfaces()).contains(Resource.class))) {
					// This is a single valued nested resource
					Object ret = template.queryForObject(MySQLQueryCreatorUtil.formRetrieveNestedObject(result, field), Object.class);
					
					if(ret != null){
						int objectId = Integer.parseInt(ret.toString());
						Object ob = field.getType().newInstance();
						MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(objectId));
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()), ob);
					}else{
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()), new Object[]{null}, new Class[] {field.getType()});
					}
					
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return result;
	}

	@Override
	@Transactional
	public Resource save(Resource resource) {
		KeyHolder holder = new GeneratedKeyHolder();
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

		Map<String, List<String>> nestedResourceKeyMap = new HashMap<>();
		Map<String, Object[]> normalArrayFieldMap = new HashMap<>();

		try {
			Map<String, Object> allFieldValues = new HashMap<>();
			for (Field field : resource.getClass().getDeclaredFields()) {

				if (field.getType().isArray() && field.getType().getComponentType().getInterfaces() != null
						&& Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class)) {

					// This is an Array of nested resources - Insert all the
					// nested resources first
					Resource[] nestedResources = (Resource[]) MethodUtils.invokeExactMethod(resource,
							getMethodName(field.getName()));
					if (nestedResources != null && nestedResources.length > 0) {
						String tableName = resource.getResourceName().toUpperCase() + "_"
								+ field.getName().toUpperCase() + "_" + "INFO";
						List<String> keyList = new ArrayList<>();
						for (Resource nestedResource : nestedResources) {
							nestedResource = save(nestedResource);
							keyList.add((String) MethodUtils.invokeExactMethod(nestedResource, "getId"));
						}
						nestedResourceKeyMap.put(tableName, keyList);
					}

				} else if (field.getType().isArray()
						&& (field.getType().getComponentType().getInterfaces() == null || !Arrays
								.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) { // Check
																															// for
																															// primitive
																															// type

					// This field is a Array of non resource objects
					String tableName = resource.getResourceName().toUpperCase() + "_" + field.getName().toUpperCase()
							+ "_DATA";
					Object[] arrayFieldValues = (Object[]) MethodUtils.invokeExactMethod(resource,
							getMethodName(field.getName()));
					if (arrayFieldValues != null && arrayFieldValues.length > 0) {
						normalArrayFieldMap.put(tableName, arrayFieldValues);
					}

				} else if (field.getType().getInterfaces() != null
						&& Arrays.asList(field.getType().getInterfaces()).contains(Resource.class)) {

					// This is an embedded resource - save and retrieve the
					// unique id first
					Resource nestedResource = (Resource) MethodUtils.invokeExactMethod(resource,
							getMethodName(field.getName()));
					if (nestedResource != null) {
						nestedResource = save(nestedResource);
						allFieldValues.put(field.getName(), MethodUtils.invokeExactMethod(nestedResource, "getId"));
					}
				} else {
					// This is a normal field - get the field value
					field.setAccessible(true);
					allFieldValues.put(field.getName(), field.get(resource));
				}
			}

			// Save the root resource
			SqlParameterSource namedParameters = new MapSqlParameterSource(allFieldValues);
			namedParameterJdbcTemplate.update(
					MySQLQueryCreatorUtil.formInsertQuery(resource.getResourceName(), resource.getResourceClass()),
					namedParameters, holder);
			MethodUtils.invokeExactMethod(resource, "setId", holder.getKey().toString());

			if (!nestedResourceKeyMap.isEmpty() || !normalArrayFieldMap.isEmpty()) {
				JdbcTemplate jdbcInsertTemplate = new JdbcTemplate(dataSource);

				// Root resource has been successfully inserted - add all array
				// of multi-valued nested resource relations
				for (Map.Entry<String, List<String>> entry : nestedResourceKeyMap.entrySet()) {
					String[] columns = entry.getKey().split("_");
					for (String nestedKey : entry.getValue()) {
						String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
								entry.getKey(), columns[0].toLowerCase(), columns[1].toLowerCase(),
								holder.getKey().toString(), nestedKey);
						jdbcInsertTemplate.execute(query);
					}
				}

				// Add all primitives
				for (Map.Entry<String, Object[]> entry : normalArrayFieldMap.entrySet()) {
					String[] columns = entry.getKey().split("_");
					for (Object arrayValue : entry.getValue()) {
						String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
								entry.getKey(), columns[0].toLowerCase(), columns[1].toLowerCase(),
								holder.getKey().toString(), "'" + arrayValue + "'");
						jdbcInsertTemplate.execute(query);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

		return resource;
	}

	@Override
	@Transactional
	public void delete(String id, String resourceName) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		template.execute(MySQLQueryCreatorUtil.formDeleteQuery(id, resourceName));
	}

	@Override
	public List<Resource> retrieveAllWithFilter(Resource resource, SearchCriteria searchCriteria,SearchCriteria sortCriteria, int batchSize, int startIndex) {
		
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Resource> results = template.query(MySQLQueryCreatorUtil.formRetriveWithFilterQuery(resource, searchCriteria,
					sortCriteria, batchSize, startIndex, searchQueryBuilder),new BeanPropertyRowMapper(resource.getResourceClass()));
		
		try{
			for (Resource result : results) {
				
				for (Field field : resource.getResourceClass().getDeclaredFields()) {

					if (field.getType().isArray() && (field.getType().getComponentType().getInterfaces() == null
							|| !Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
						// This is an array of simple type - fetch and add to resource
						List arrayValues = template.queryForList(
								MySQLQueryCreatorUtil.formRetrieveSimpleArrayQuery(result, field),
								field.getType().getComponentType());
						field.setAccessible(true);
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
								(Object) arrayValues.toArray((Object[]) Array.newInstance(field.getType().getComponentType(), arrayValues.size())));
					} else if (field.getType().isArray() && (field.getType().getComponentType().getInterfaces() != null
							&& Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class))) {
						// This is an array of nested type - fetch and add nested type
						List<Integer> arrayValues = template.queryForList(
								MySQLQueryCreatorUtil.formRetrieveNestedArrayQuery(result, field), int.class);
						List<Object> nestedObjects = new ArrayList<>();

						for (int nestedId : arrayValues) {
							Object ob = field.getType().getComponentType().newInstance();
							MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(nestedId));
							nestedObjects.add(ob);
						}

						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()),
								(Object) nestedObjects.toArray((Object[]) Array
										.newInstance(field.getType().getComponentType(), nestedObjects.size())));
					} else if (!field.getType().isArray() && (field.getType().getInterfaces() != null
							&& Arrays.asList(field.getType().getInterfaces()).contains(Resource.class))) {
						// This is a single valued nested resource
						int objectId = template.queryForObject(
								MySQLQueryCreatorUtil.formRetrieveNestedObject(result, field), int.class);
						Object ob = field.getType().newInstance();
						MethodUtils.invokeExactMethod(ob, "setId", String.valueOf(objectId));
						MethodUtils.invokeExactMethod(result, formSetMethodname(field.getName()), ob);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		
		return results;
	}

	@Override
	@Transactional
	public Resource update(String id, Resource resource, Map<String, Map<String, List<Object>>> operationsMap) {
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(resource);
		namedParameterJdbcTemplate.update(MySQLQueryCreatorUtil.formUpdateQuery(resource), namedParameters);
		
		try{
		
			JdbcTemplate jdbcInsertTemplate = new JdbcTemplate(dataSource);
			for(Entry<String, Map<String, List<Object>>> entry : operationsMap.entrySet()){
					
				Field field = resource.getResourceClass().getDeclaredField(entry.getKey());
					if(!Arrays.asList(field.getType().getComponentType().getInterfaces()).contains(Resource.class)){
						for(Entry<String, List<Object>> opValueEntry : entry.getValue().entrySet()){
							for(Object value : opValueEntry.getValue()){
								
								if(opValueEntry.getKey().equals("add")){
									String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
										                     resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_DATA", 
										                     resource.getResourceName().toLowerCase(),
										                     entry.getKey().toLowerCase(),
										                     resource.getId(), value);
									jdbcInsertTemplate.execute(query);
								}else{
									String query = String.format(MySQLQueryCreatorUtil.formUpdateQueryForRemoveArrayValue(),
											                     resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_DATA", 
											                     resource.getResourceName().toLowerCase(),
											                     resource.getId(),
											                     entry.getKey().toLowerCase(),
											                     value);
									jdbcInsertTemplate.execute(query);
								}
							}
						}
					}else{
						for(Entry<String, List<Object>> opValueEntry : entry.getValue().entrySet()){
							for(Object value : opValueEntry.getValue()){
								
								if(opValueEntry.getKey().equals("add")){
									String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
										                         resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_INFO", 
										                         resource.getResourceName().toLowerCase(),
										                         entry.getKey().toLowerCase(),
										                         resource.getId(), value);
									jdbcInsertTemplate.execute(query);
								}else{
									String query = String.format(MySQLQueryCreatorUtil.formUpdateQueryForRemoveArrayValue(),
											                     resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_INFO", 
											                     resource.getResourceName().toLowerCase(),
											                     resource.getId(),
											                     entry.getKey().toLowerCase(),
											                     value);
									jdbcInsertTemplate.execute(query);
								}
							}
						}
					}
					
			}
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
		return resource;
	}

	private static String getMethodName(String key) {
		key = key.toUpperCase().charAt(0) + key.substring(1);
		return "get" + key;
	}

	private static String formSetMethodname(String fieldName) {
		fieldName = fieldName.toUpperCase().charAt(0) + fieldName.substring(1);
		return "set" + fieldName.replace("()", "");
	}

}

class RelationshipKeyHolder {
	private String key1;
	private String key2;

	public RelationshipKeyHolder(String key1, String key2) {
		this.key1 = key1;
		this.key2 = key2;
	}

	public String getKey1() {
		return key1;
	}

	public String getKey2() {
		return key2;
	}

}
