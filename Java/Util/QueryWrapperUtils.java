package com.masget.command.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.alibaba.fastjson.JSONObject;


/**
 * Mybatis 查询包装
 * @author hoojo
 * @createDate 2017年8月29日 上午11:04:23
 * @file QueryWrapperUtils.java
 * @blog http://blog.csdn.net/IBM_hoojo
 * @email hoojo_@126.com
 * @version 1.0
 */
public abstract class QueryWrapperUtils {

	public static QueryWrapper builder(List<String> cols, List<String> orderCols, String sortMode, Integer pageSize, Integer pageNo) {
		return new QueryWrapper(cols, orderCols, sortMode, pageSize, pageNo);
	}
	
	public static QueryWrapper builder(String cols, String orderCols, String sortMode, Integer pageSize, Integer pageNo) {
		return new QueryWrapper(Arrays.asList(StringUtils.split(cols, ",")), Arrays.asList(StringUtils.split(orderCols, ",")), sortMode, pageSize, pageNo);
	}
	
	public static QueryWrapper builder(JSONObject param) {
		Integer pageSize = null;
		if (param.containsKey("pagesize")) {
			pageSize = param.getInteger("pagesize");
		} else {
			pageSize = param.getInteger("pageSize");
		}
		
		Integer pageNo = null;
		if (param.containsKey("pagenum")) {
			pageNo = param.getInteger("pagenum");
		} else {
			pageNo = param.getInteger("pageNo");
		}
		
		return new QueryWrapper(Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(param.getString("cols"), ""), ",")), 
								Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(param.getString("orderCols"), ""), ",")), 
								param.getString("sortMode"), pageSize, pageNo);
	}
	
	public static QueryWrapper builder(String cols, String orderCols, String sortMode) {
		return builder(cols, orderCols, sortMode, null, null);
	}
	
	public static QueryWrapper builder(String cols, Integer pageSize, Integer pageNo) {
		return builder(cols, "", null, pageSize, pageNo);
	}
	
	public static QueryWrapper builder(String orderCols, String sortMode) {
		return builder("", orderCols, sortMode, null, null);
	}
	
	public static QueryWrapper builder(Integer pageSize, Integer pageNo) {
		return new QueryWrapper(null, null, null, pageSize, pageNo);
	}
	
	public static QueryWrapper builder(String cols) {
		return builder(cols, "", null, null, null);
	}
	
	public static enum SortMode {
		DESC, ASC
	}
	
	public static class QueryWrapper {
		
		private List<String> cols; 
		private List<String> orderCols; 
		private String sortMode;
		
		private Integer pageSize;
		private Integer pageNo;
		
		public QueryWrapper() {
		}
		
		public QueryWrapper(List<String> cols, List<String> orderCols, String sortMode, Integer pageSize, Integer pageNo) {
			super();
			this.cols = cols;
			this.orderCols = orderCols;
			this.sortMode = sortMode;
			this.pageSize = pageSize;
			this.pageNo = pageNo;
		}

		public List<String> getCols() {
			return cols;
		}
		public void setCols(List<String> cols) {
			this.cols = cols;
		}
		public List<String> getOrderCols() {
			return orderCols;
		}
		public void setOrderCols(List<String> orderCols) {
			this.orderCols = orderCols;
		}
		public String getSortMode() {
			return sortMode;
		}
		public void setSortMode(String sortMode) {
			this.sortMode = sortMode;
		}
		public Integer getPageSize() {
			return pageSize;
		}
		public void setPageSize(Integer pageSize) {
			this.pageSize = pageSize;
		}
		public Integer getPageNo() {
			return pageNo;
		}
		public void setPageNo(Integer pageNo) {
			this.pageNo = pageNo;
		}
		
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}
}


