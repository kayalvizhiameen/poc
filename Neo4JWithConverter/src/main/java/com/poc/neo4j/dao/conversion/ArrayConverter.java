package com.poc.neo4j.dao.conversion;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Node;

import com.poc.neo4j.dao.Constants;
import com.poc.neo4j.dao.exception.ConverterException;
import com.poc.neo4j.dao.util.GraphDbUtil;
import com.poc.neo4j.dao.util.ReflectionUtil;
import com.poc.neo4j.model.BaseEntity;

/**
 * Conversion of a {@link Node} property value to any entity field value
 * if the entity's field is of array type
 *  
 * @author kayalv
 *
 */
public class ArrayConverter implements PropertyConverter {

	private static final Logger LOGGER = Logger.getLogger(ArrayConverter.class);
	
	@Override
	public <T> void marshall(T source, Node destination, String fieldName, Object sourceValue)
			throws ConverterException {
		
		Object[] objects = (Object[]) sourceValue;
		if (Object.class.getName().equals(sourceValue.getClass().getComponentType().getName())){
			LOGGER.error(Constants.ERROR_TYPE);
			System.err.println(Constants.ERROR_TYPE);
			return;
		}
		Object[] complexTypeArray = null;
		boolean nullValuePresentInArray = false;
		for (int i = 0; i < objects.length; i++) {
			Object value =  objects[i];
			if (!ReflectionUtil.isSimpleType(value)) {
				
				if (complexTypeArray == null) {
					complexTypeArray = (Object[]) Array.newInstance(value.getClass(), objects.length);
				}
				try{
					if (value instanceof BaseEntity){
						complexTypeArray[i] = value;
					} else {
						LOGGER.error(Constants.ERROR_OBJECT_TYPE);
						System.err.println(Constants.ERROR_OBJECT_TYPE);
					}
				} catch(ArrayStoreException e) {
					String error = Constants.ERROR_TYPE + ".\nExpected type is " +  
							complexTypeArray.getClass().getComponentType()
							+ " But the object " + value + "[" + i + "] is of different type ";
					LOGGER.error(error);
					System.err.println(error);
					nullValuePresentInArray = true;
				}
			}
		}
		
		if (complexTypeArray == null) {
			destination.setProperty(fieldName, objects);
		} else if (complexTypeArray != null && objects.length == complexTypeArray.length && !nullValuePresentInArray) {
			for (int i = 0; i < complexTypeArray.length; i++) {
				GraphDbUtil.convertAndPersistNode((BaseEntity) complexTypeArray[i], destination, fieldName);
			}
		} else {
			LOGGER.error(Constants.ERROR_TYPE);
			System.err.println(Constants.ERROR_TYPE);
		}
	}

	@Override
	public <T> void unmarshall(Node source, T destination, String propertyName, T unmarshalledChild)
			throws ConverterException {
		
		Class<?> componentType = ReflectionUtil.getType(destination.getClass(), propertyName).getComponentType();
		if (ReflectionUtil.isSimpleType(componentType)) {
			ReflectionUtil.setProperty(destination, propertyName, source.getProperty(propertyName));
		} else if (unmarshalledChild != null) {
			int index = -1;
			Object objects =  ReflectionUtil.getProperty(destination, propertyName);
			if (objects == null) {
				objects = Array.newInstance(componentType, 0);
				ReflectionUtil.setProperty(destination, propertyName, objects);
			}
			index = ((Object[])objects).length;
			if (((Object[])objects).length < index  + 1) {
				objects = Arrays.copyOf((Object[]) objects, index + 1);
				ReflectionUtil.setProperty(destination, propertyName, objects);
			}
			((Object[])objects)[index] = unmarshalledChild;
		}
	}

}
