package com.bamboo.jdbc;

import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bamboo.core.SearchCriteria;
import com.bamboo.jdbc.util.MySQLQueryCreatorUtil;

@Repository("mySQLPersistanceHelperImpl")
public class MySQLPersistanceHelperImpl implements PersistanceHelper {
	
	@Autowired(required = true)
	private DataSource dataSource;
	
	@Autowired(required = true)
	private SearchQueryBuilder searchQueryBuilder;
	
	@Override
	public List<Object> retrieveAll(String resourceName, Class resourceType) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Object> results = template.query(MySQLQueryCreatorUtil.formRetrieveAllQuery(resourceName), new BeanPropertyRowMapper(resourceType));
		return results;
	}

	@Override
	public Object retrieveByID(String id, String resourceName, Class resourceType) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Object> results = template.query(MySQLQueryCreatorUtil.formRetrieveByIDQuery(id, resourceName), new BeanPropertyRowMapper(resourceType));
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	@Transactional
	public Object save(Object resource, String resourceName, Class resourceType) {
		KeyHolder holder = new GeneratedKeyHolder();
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(resource);
		namedParameterJdbcTemplate.update(MySQLQueryCreatorUtil.formInsertQuery(resourceName, resourceType), namedParameters, holder);
		try {
			MethodUtils.invokeExactMethod(resource, "setId", holder.getKey().toString());
		} catch (Exception e) {
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
	public List<Object> retrieveAllWithFilter(String resourceName, Class resourceType, SearchCriteria searchCriteria, SearchCriteria sortCriteria, int batchSize, int startIndex) {
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Object> results = template.query(MySQLQueryCreatorUtil.formRetriveWithFilterQuery(resourceName, searchCriteria, sortCriteria, batchSize, startIndex, searchQueryBuilder), new BeanPropertyRowMapper(resourceType));
		return results;
	}

	@Override
	@Transactional
	public Object update(String id, String resourceName, Class resourceType, Object resource) {
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		SqlParameterSource namedParameters = new BeanPropertySqlParameterSource(resource);
		namedParameterJdbcTemplate.update(MySQLQueryCreatorUtil.formUpdateQuery(resourceName, resourceType), namedParameters);
		return resource;
	}

}
