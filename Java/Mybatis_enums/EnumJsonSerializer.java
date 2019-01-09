package com.masget.command.mybatis;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.masget.command.utils.BeanMapUtils;

/**
 * <b>function:</b> 枚举转换json
 * 
 * @author hoojo
 * @createDate 2017-3-18 下午2:15:27
 * @file EnumJsonSerializer.java
 * @blog http://blog.csdn.net/IBM_hoojo
 * @email hoojo_@126.com
 * @version 1.0
 */
public class EnumJsonSerializer implements ObjectSerializer {

	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
		if (object == null) {
			return;
		}
		
		if (ClassUtils.isAssignable(object.getClass(), Enum.class) && object instanceof BaseEnumInterface) {
			Enum<?> enums = (Enum<?>) object;
			
			Map<String, Object> map = BeanMapUtils.transBean2Map(enums);
            map.put("_name", enums.name());
            map.remove("class");
            map.remove("declaringClass");
			
			serializer.write(map);
			//serializer.write(enums.getCode());
		} else {
			serializer.write(object);
		}
	}
}
