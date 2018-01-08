package com.hoojo.command.utils;

import java.util.Collection;
import java.util.Map;

/**
 * 判断MyBatis值是否为空
 * 
 * @author hoojo
 * @createDate 2017-5-24 下午5:21:23
 * @file OgnlUtils.java
 * @email hoojo_@126.com
 * @version 1.0
 */
public abstract class OgnlUtils {
	
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object o) {
		if (o == null)
			return true;
		if (o instanceof String) {
			if (((String) o).trim().length() == 0)
				return true;
		} else if (o instanceof Collection) {
			if (((Collection) o).isEmpty())
				return true;
		} else if (o.getClass().isArray()) {
			if (((Object[]) (Object[]) o).length == 0)
				return true;
		} else if (o instanceof Map) {
			if (((Map) o).isEmpty())
				return true;
		} else {
			return false;
		}
		return false;
	}

	public static boolean isNotEmpty(Object o) {
		return (!(isEmpty(o)));
	}
}
