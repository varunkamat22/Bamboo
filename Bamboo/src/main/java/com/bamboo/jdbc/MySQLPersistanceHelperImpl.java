package com.bamboo.jdbc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bamboo.core.Resource;
import com.bamboo.core.SearchCriteria;
import com.bamboo.core.types.ResourceWrapper;
import com.bamboo.core.util.ResourceConstants;
import com.bamboo.jdbc.util.MySQLQueryCreatorUtil;

@Repository("mySQLPersistanceHelperImpl")
public class MySQLPersistanceHelperImpl implements PersistanceHelper {

	@Autowired(required = true)
	private DataSource dataSource;

	@Autowired(required = true)
	private SearchQueryBuilder searchQueryBuilder;

	@Override
	public List<Resource> retrieveAll(Resource resource) {

		try {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			List<Resource> results = template.query(MySQLQueryCreatorUtil.formRetrieveAllQuery(resource), new BeanPropertyRowMapper(resource.getClass()));

			for (Resource result : results) {
				populateResourceObject(result, template);
			}
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		
	}

	@Override
	public Resource retrieveByID(String id, Resource resource) {
		try {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			List<Resource> results = template.query(MySQLQueryCreatorUtil.formRetrieveByIDQuery(id, resource),new BeanPropertyRowMapper(resource.getResourceClass()));
			
			if(results.isEmpty())
				return null;
			else
				return populateResourceObject(results.get(0), template);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	private Resource populateResourceObject(Resource resource, JdbcTemplate template){
		
		ResourceWrapper resourceWrapper = new ResourceWrapper(resource);
		
		if(!resourceWrapper.getSimpleArrayFields().isEmpty()){
			// This is an array of simple type - fetch and add to resource
			for(Field field : resourceWrapper.getSimpleArrayFields()){
				List<?> valueList = template.queryForList(MySQLQueryCreatorUtil.formRetrieveSimpleArrayQuery(resource, field),
						resourceWrapper.getComponentType(field.getName()));
				resourceWrapper.setFieldValue(field.getName(), valueList);
			}
		}
		
		if(!resourceWrapper.getNestedResourceArrayFields().isEmpty()){
			// This is an array of nested type - fetch and add nested type
			for(Field field : resourceWrapper.getNestedResourceArrayFields()){
				List<Integer> nestedIds = template.queryForList(MySQLQueryCreatorUtil.formRetrieveNestedArrayQuery(resource, field), int.class);
				List<Object> nestedObjects = new ArrayList<>();
				
				nestedIds.forEach(nestedResID -> nestedObjects.add(resourceWrapper.getComponentInstance(field, nestedResID)));
				resourceWrapper.setFieldValue(field.getName(), nestedObjects);
			}
		}
		
		if(!resourceWrapper.getNestedResourceFields().isEmpty()){
			// This is a single valued nested resource
			for(Field field : resourceWrapper.getNestedResourceFields()){
				int nestedResourceId = template.queryForObject(MySQLQueryCreatorUtil.formRetrieveNestedObject(resource, field), int.class);
				resourceWrapper.setFieldValue(field.getName(), resourceWrapper.getComponentInstance(field, nestedResourceId));
			}
	    }
		
		return resourceWrapper.getResource();
	}
	
	@Override
	public List<Resource> retrieveAllWithFilter(Resource resource, SearchCriteria searchCriteria,SearchCriteria sortCriteria, int batchSize, int startIndex) {
		try{
			JdbcTemplate template = new JdbcTemplate(dataSource);
			List<Resource> results = template.query(MySQLQueryCreatorUtil.formRetriveWithFilterQuery(resource, searchCriteria, sortCriteria, batchSize, startIndex, searchQueryBuilder),new BeanPropertyRowMapper(resource.getResourceClass()));
		
			for (Resource result : results) {
				populateResourceObject(result, template);
			}
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	@Transactional
	public Resource save(Resource resource) {
		KeyHolder holder = new GeneratedKeyHolder();
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

		Map<String, List<String>> nestedResourceArrayKeyMap = new HashMap<>();
		Map<String, Object> simpleAndResourceFieldValues = new HashMap<>();

		try {
			ResourceWrapper resourceWrapper = new ResourceWrapper(resource);
			
			if(!resourceWrapper.getNestedResourceArrayFields().isEmpty()){
				// This is an array of nested type - fetch and add nested type
				for(Field field : resourceWrapper.getNestedResourceArrayFields()){
					Resource[] nestedResourceObjects = (Resource[]) resourceWrapper.getFieldValue(field.getName());
					if(nestedResourceObjects != null && nestedResourceObjects.length != 0){
						String tableName = resource.getResourceName().toUpperCase() + "_" + field.getName().toUpperCase() + "_" + "INFO";
						List<String> nestedResourceKeys = new ArrayList<>();
						for (Resource nestedResourceObject : nestedResourceObjects) {
							nestedResourceObject = save(nestedResourceObject);
							nestedResourceKeys.add(nestedResourceObject.getId());
						}
						nestedResourceArrayKeyMap.put(tableName, nestedResourceKeys);
					}
				}
			}
			
			if(!resourceWrapper.getNestedResourceFields().isEmpty()){
				// This is a single valued nested resource
				for(Field field : resourceWrapper.getNestedResourceFields()){
					Resource nestedResource = (Resource)resourceWrapper.getFieldValue(field.getName());
					if (nestedResource != null) {
						nestedResource = save(nestedResource);
						simpleAndResourceFieldValues.put(field.getName(), nestedResource.getId());
					}
				}
		    }
			
			if(!resourceWrapper.getSimpleFields().isEmpty()){
				// This is a simple field
				for(Field field : resourceWrapper.getSimpleFields()){
					simpleAndResourceFieldValues.put(field.getName(), resourceWrapper.getFieldValue(field.getName()));
				}
			}
			
			
			// Save the Root resource
			SqlParameterSource namedParameters = new MapSqlParameterSource(simpleAndResourceFieldValues);
			namedParameterJdbcTemplate.update(MySQLQueryCreatorUtil.formInsertQuery(resource.getResourceName(), resource.getResourceClass()),
					                          namedParameters, holder);
			resourceWrapper.setFieldValue("id", holder.getKey().toString());
			
			JdbcTemplate jdbcInsertTemplate = new JdbcTemplate(dataSource);
			
			if (!nestedResourceArrayKeyMap.isEmpty()) {
				// Root resource has been successfully inserted - add all key relationships for nested resource arrays 
				for (Map.Entry<String, List<String>> entry : nestedResourceArrayKeyMap.entrySet()) {
					String[] columns = entry.getKey().split("_");
					for (String nestedResourceKey : entry.getValue()) {
						String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
								                     entry.getKey(), columns[0].toLowerCase(), columns[1].toLowerCase(),
								                     holder.getKey().toString(), nestedResourceKey);
						jdbcInsertTemplate.execute(query);
					}
				}
			}
			
			if(!resourceWrapper.getSimpleArrayFields().isEmpty()){
				// This is an array of simple type - fetch and add to resource
				for(Field field : resourceWrapper.getSimpleArrayFields()){
					String tableName = resource.getResourceName().toUpperCase() + "_" + field.getName().toUpperCase() + "_DATA";
					Object[] arrayFieldValues = (Object[]) resourceWrapper.getFieldValue(field.getName());
					if (arrayFieldValues != null && arrayFieldValues.length > 0) {
						for(Object arrayFieldValue : arrayFieldValues){
							String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
									                     tableName, 
									                     resource.getResourceName().toLowerCase(), 
									                     field.getName().toLowerCase(),
									                     resource.getId(), 
									                     "'" + arrayFieldValue + "'");
							jdbcInsertTemplate.execute(query);	
						}
					}
				}
			}			
			
			return resourceWrapper.getResource();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

	}

	@Override
	@Transactional
	public void delete(String id, Resource resource) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		
		ResourceWrapper resourceWrapper = new ResourceWrapper(resource);
		if(!resourceWrapper.getSimpleArrayFields().isEmpty()){
		    //Delete all entries for Simple Array fields
			for(Field field : resourceWrapper.getSimpleArrayFields()){
				String arrayDelQuery = String.format(MySQLQueryCreatorUtil.formDeleteQuery() , 
													 resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_DATA", 
													 resource.getResourceName().toLowerCase(), id);
				template.execute(arrayDelQuery);
			}
		}
		
		if(!resourceWrapper.getNestedResourceArrayFields().isEmpty()){
			//Delete all entries for Resource Array fields
			for(Field field : resourceWrapper.getNestedResourceArrayFields()){
				String arrayDelQuery = String.format(MySQLQueryCreatorUtil.formDeleteQuery() , 
													 resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_INFO", 
													 resource.getResourceName().toLowerCase(), id);
				template.execute(arrayDelQuery);
			}
		}
		
		//Delete the actual resource
		String query = String.format(MySQLQueryCreatorUtil.formDeleteQuery() , resource.getResourceName().toUpperCase(), "id", id);
		template.execute(query);
		
	}

	@Override
	@Transactional
	public Resource update(String id, Resource resource, Map<String, Map<String, List<Object>>> operationsMap) {
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		
		try{
			ResourceWrapper resourceWrapper = new ResourceWrapper(resource);
			
			SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(resource);
			namedParameterJdbcTemplate.update(MySQLQueryCreatorUtil.formUpdateQuery(resource), namedParameters);
			
			JdbcTemplate jdbcInsertTemplate = new JdbcTemplate(dataSource);
			//First clear out all array entries that are set to null
			
			if(!resourceWrapper.getSimpleArrayFields().isEmpty()){
				for(Field field : resourceWrapper.getSimpleArrayFields()){
					if(resourceWrapper.getFieldValue(field.getName()) == null){
						String query = String.format(MySQLQueryCreatorUtil.formClearArrayQuery(),
                                                     resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_DATA",
                                                     resource.getResourceName().toLowerCase(), id);
						jdbcInsertTemplate.execute(query);
					}
				}
			}
			
			if(!resourceWrapper.getNestedResourceArrayFields().isEmpty()){
				for(Field field : resourceWrapper.getNestedResourceArrayFields()){
					if(resourceWrapper.getFieldValue(field.getName()) == null){
						String query = String.format(MySQLQueryCreatorUtil.formClearArrayQuery(),
													 resource.getResourceName().toUpperCase()+"_"+field.getName().toUpperCase()+"_INFO",
													 resource.getResourceName().toLowerCase(), id);
						jdbcInsertTemplate.execute(query);
					}
				}
			}
			

			for(Entry<String, Map<String, List<Object>>> entry : operationsMap.entrySet()){
				if(resourceWrapper.isSimpleArray(entry.getKey())){
					
					for(Entry<String, List<Object>> opValueEntry : entry.getValue().entrySet()){
						for(Object value : opValueEntry.getValue()){
							
							if(opValueEntry.getKey().equals(ResourceConstants.OPERATIONS_ADD)){
								String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
									                         resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_DATA", 
									                         resource.getResourceName().toLowerCase(),
									                         entry.getKey().toLowerCase(), id, value);
								jdbcInsertTemplate.execute(query);
							}else{
								String query = String.format(MySQLQueryCreatorUtil.formUpdateQueryForRemoveArrayValue(),
										                     resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_DATA", 
										                     resource.getResourceName().toLowerCase(),
										                     id, entry.getKey().toLowerCase(), value);
								jdbcInsertTemplate.execute(query);
							}
						}
					}
				} else{
					
					for(Entry<String, List<Object>> opValueEntry : entry.getValue().entrySet()){
						for(Object value : opValueEntry.getValue()){
							
							if(opValueEntry.getKey().equals(ResourceConstants.OPERATIONS_ADD)){
								String query = String.format(MySQLQueryCreatorUtil.formInsertQueryForLinkedTable(),
									                         resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_INFO", 
									                         resource.getResourceName().toLowerCase(),
									                         entry.getKey().toLowerCase(), id, value);
								jdbcInsertTemplate.execute(query);
							}else{
								String query = String.format(MySQLQueryCreatorUtil.formUpdateQueryForRemoveArrayValue(),
										                     resource.getResourceName().toUpperCase()+"_"+entry.getKey().toUpperCase()+"_INFO", 
										                     resource.getResourceName().toLowerCase(),
										                     id, entry.getKey().toLowerCase(), value);
								jdbcInsertTemplate.execute(query);
							}
						}
					}
				}
			}
			
			return resourceWrapper.getResource();
		}catch(Exception ex){
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		
	}

}


