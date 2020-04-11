package com.hoojo.command.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hoojo.command.enums.AccountType;
import com.hoojo.command.mybatis.BaseEnumInterface;
import com.hoojo.enums.base.EnableFlag;

/**
 * 枚举值转换枚举对象
 * @author hoojo
 * @createDate 2017年8月30日 下午12:55:47
 * @file EnumUtils.java
 * @email hoojo_@126.com
 * @version 1.0
 */
public abstract class EnumUtils {

	private static Logger logger = LoggerFactory.getLogger(EnumUtils.class);
	
	/**
	 * <b>function:</b> 通过枚举类型和枚举属性、属性值，转换到该值对应的枚举对象
	 * @author hoojo
	 * @createDate 2017年8月30日 下午12:58:15
	 * @param enumClass 枚举类型
	 * @param field 枚举里面定义的属性
	 * @param value 枚举里面定义属性传入的一个值
	 * @return 当前传入枚举类型对应的枚举对象
	 * @throws Exception
	 */
	public static <T> T castEnumType(Class<T> enumClass, String field, Object value) throws Exception {
    	if (!enumClass.isEnum()) {
    		return null;
    	}
    	
    	T[] enumConstants = enumClass.getEnumConstants();
    	
    	Map<Object, T> enumTypes = new HashMap<Object, T>();
        for (T cost : enumConstants) {

        	Field code = cost.getClass().getDeclaredField(field);
        	if (code == null) {
        		logger.error("枚举工具报错：没有找到对应的{}属性", field);
        		return null;
        	}
            code.setAccessible(true);
            enumTypes.put(code.get(cost).toString(), cost);
        }
        
    	return enumTypes.get(ObjectUtils.toString(value));
    }
	
	/**
	 * <b>function:</b> 通过枚举类型、属性值，转换到该值对应的枚举对象
	 * @author hoojo
	 * @createDate 2017年8月30日 下午12:58:15
	 * @param enumClass 枚举类型
	 * @param value 枚举里面定义属性传入的一个值
	 * @return 当前传入枚举类型对应的枚举对象
	 * @throws Exception
	 */
	public static <T> T castEnumType(Class<T> enumClass, Object value) throws Exception {
    	return castEnumType(enumClass, "code", value);
    }
	
	public static <T> T castEnumType(Class<?> interfaceClass, Class<T> enumClass, String field, Object value) throws Exception {
		if (enumClass.getInterfaces().length > 0 && ClassUtils.isAssignable(enumClass.getInterfaces()[0], interfaceClass)) {
			return castEnumType(enumClass, field, value);
		} else {
			logger.error("枚举：{} 未实现接口：{}", enumClass, interfaceClass);
			return null;
		}
    }
	
	public static void main(String[] args) throws Exception {
		System.out.println(EnumUtils.castEnumType(EnableFlag.class, "id", 1).getName());
		System.out.println(EnumUtils.castEnumType(AccountType.class, 0).getName());
		System.out.println(EnumUtils.castEnumType(BaseEnumInterface.class, AccountType.class, "value", 0).getName());
	}
}
