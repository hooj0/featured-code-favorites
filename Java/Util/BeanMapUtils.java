package com.masget.command.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 针对数据库设计规则，对空值问题进行处理
 * 
 * @author hoojo
 * @createDate 2017-3-13 下午7:48:40
 * @file BeanMapUtils.java
 * @email hoojo_@126.com
 * @version 1.0
 */
public abstract class BeanMapUtils {

	private static Logger logger = LoggerFactory.getLogger(BeanMapUtils.class);
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	static {
		ConvertUtils.register(new Converter() {
			public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
				if (ClassUtils.isAssignable(type, Date.class) && value != null) {
					try {
						return DATE_FORMAT.parse(value.toString());
					} catch (ParseException e) {
						logger.error(e.getMessage());
					}
				}
				return null;
			}
		}, Date.class);
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void transMap2Bean(Map<String, Object> map, Object bean) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    if (value == null) {
                    	continue;
                    }
                    
                    Method setter = property.getWriteMethod();
                    
                    try {
                    	if (ClassUtils.isAssignable(setter.getParameterTypes()[0], Enum.class)) {
                    		logger.trace("Enum Type：{}", setter.getParameterTypes()[0].getName());
                    		
                    		Object enumType = EnumUtils.castEnumType(setter.getParameterTypes()[0], value);
                    		if (enumType == null && ClassUtils.isAssignable(value.getClass(), String.class)) {
                    			value = Enum.valueOf((Class) setter.getParameterTypes()[0], value.toString());
                    		} else if (enumType != null) {
                    			value = enumType;
                    		}
                    	} 
                    	
                    	//setter.invoke(bean, new Object[] { value });
                    	setter.invoke(bean, new Object[] { ConvertUtils.convert(value, setter.getParameterTypes()[0]) });
                    } catch (Exception e) {
                    	logger.error("不可转换的数据：{}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map<String, Object> transBean2Map(Object bean) {
        Map map = new HashMap();
        if (bean == null)
            return map;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (key.equals("class"))
                    continue;
                Method getter = property.getReadMethod();
                Object value = getter.invoke(bean, new Object[0]);
                
                if (value == null) {
                    if (getter.getReturnType() == String.class) {
                        value = "";
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Long.class)) {
                        value = 0L;
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Double.class)) {
                        value = 0D;
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Integer.class)) {
                        value = 0;
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Float.class)) {
                        value = 0F;
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Short.class)) {
                        value = 0;
                    } else if (ClassUtils.isAssignable(getter.getReturnType(), Enum.class)) {
                        value = null;
                    } 
                }
                map.put(key, value);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return map;
    }
    
    public static Map<String, String> desc(Object bean) {
    	Map<String, String> map = new HashMap<String, String>();
		if (bean == null) {
			return map;
		}

		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor property : propertyDescriptors) {
				String key = property.getName();
				if (key.equals("class"))
					continue;
				Method getter = property.getReadMethod();
				Object value = getter.invoke(bean, new Object[0]);
				if (value != null) {
					map.put(key, String.valueOf(value));
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return map;
	}
}
