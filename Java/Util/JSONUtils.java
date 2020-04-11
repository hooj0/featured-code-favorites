package com.hooj0.command.utils;

import org.apache.commons.lang.ClassUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.hooj0.command.mybatis.BaseEnumInterface;
import com.hooj0.command.mybatis.EnumJsonSerializer;
import com.hooj0.entity.RetStruct;

/**
 * fastjson 工具类
 * @author hoojo
 * @createDate 2017年9月1日 上午10:05:37
 * @file JSONUtils.java
 * @blog http://hoojo.cnblogs.com
 * @email hoojo_@126.com
 * @version 1.0
 */
@SuppressWarnings("unused")
public abstract class JSONUtils {

	private static SerializeFilter[] filters;
	private static SerializerFeature[] features;
	private static SerializeConfig config;
	
	static {
		JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		
		filters = new SerializeFilter[] {
				new JSONValueFilter()
		};
		
		features = new SerializerFeature[] {
				SerializerFeature.WriteDateUseDateFormat
		};
	}
	
	public static String toJson(Object object) throws Exception {
		return JSON.toJSONString(object, features);
	}
	
	public static String toJson(RetStruct object) throws Exception {
		if (object == null) {
			return null;
		}
		return JSON.toJSONString(object.getResult(), features);
	}
	
	private static class JSONValueFilter implements ValueFilter {
		@SuppressWarnings("rawtypes")
		public Object process(Object object, String name, Object value) {
			
			if (value == null) {
				return null;
			}
			if (ClassUtils.isAssignable(object.getClass(), Enum.class) && object instanceof BaseEnumInterface) {
				BaseEnumInterface enums = (BaseEnumInterface) object;
				
				return enums.getCode();
			} 
			return value;
		}
	}
}
