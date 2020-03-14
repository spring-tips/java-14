package com.example.fourteen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
	* Maps data from a JDBC {@code ResultSet} into Java 14 record objects of type T.
	*
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class RecordRowMapper<T> implements RowMapper<T> {

	private final Log logger = LogFactory.getLog(getClass());
	private final Map<String, RecordComponent> mappedFields = new HashMap<>();
	private final List<String> parameterNames = new ArrayList<>();
	private final Constructor<?> constructor;

	/**
		* Instantiate a new instance using the default constructor for a class.
		*/
	public RecordRowMapper(Class<T> tClass) {
		this(tClass.getConstructors()[0]);
	}

	/**
		* If there's ambiguity as to which constructor to invoke, provide one explicitly.
		*/
	public RecordRowMapper(Constructor<?> constructor) {
		Assert.notNull(constructor, "the Constructor reference should not be null");
		this.constructor = constructor;

		for (RecordComponent recordComponent : constructor.getDeclaringClass().getRecordComponents()) {
			this.mappedFields.put(normalizeName(recordComponent.getName()), recordComponent);
		}

		for (Parameter parameter : constructor.getParameters()) {
			parameterNames.add(parameter.getName());
		}
	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		Map<String, Object> objectProperties = new HashMap<>();
		int columnCount = metaData.getColumnCount();
		for (int index = 1; index <= columnCount; index++) {
			String column = JdbcUtils.lookupColumnName(metaData, index);
			RecordComponent recordComponent = this.mappedFields.get(normalizeName(column));
			if (null != recordComponent) {
				if (rowNum == 0 && logger.isDebugEnabled()) {
					logger.debug("Mapping column '" + column + "' to property '" + recordComponent.getName() +
						"' of type '" + ClassUtils.getQualifiedName(recordComponent.getDeclaringRecord()) + "'");
				}
				objectProperties.put(recordComponent.getName(), getColumnValue(rs, index, recordComponent.getType()));
			}
		}
		try {
			return (T) this.constructor.newInstance(
				this.parameterNames.stream().map(objectProperties::get).toArray()
			);
		}
		catch (Exception e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}
		return null;
	}

	private String normalizeName(String name) {
		return name.toLowerCase().replaceAll("_", "");
	}

	private Object getColumnValue(ResultSet rs, int index, Class<?> clzz) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index, clzz);
	}
}