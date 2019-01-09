package com.masget.command.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 枚举类型转换处理器
 * 
 * @author hoojo
 * @createDate 2017-3-13 上午9:51:06
 * @file GeneralEnumHandler.java
 * @email hoojo_@126.com
 * @version 1.0
 */
public class GeneralEnumHandler<E extends BaseEnumInterface<Enum<?>, Integer>> extends BaseTypeHandler<E> {

	private static final Logger logger = LoggerFactory.getLogger(GeneralEnumHandler.class);

    private Class<E> type;

    private E[] enums;

    /**
     * 设置配置文件设置的转换类以及枚举类内容，供其他方法更便捷高效的实现
     * 
     * @param type
     *            配置文件中设置的转换类
     */
    public GeneralEnumHandler(Class<E> type) {
        if (type == null)
            throw new IllegalArgumentException("Type argument cannot be null");
        this.type = type;
        this.enums = type.getEnumConstants();
        if (this.enums == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter.getCode(), jdbcType.TYPE_CODE);
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int i = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        } else {
            return valueOf(i);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    	int i = rs.getInt(columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return valueOf(i);
        }
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int i = cs.getInt(columnIndex);
        if (cs.wasNull()) {
            return null;
        } else {
            return valueOf(i);
        }
    }

    /**
     * 枚举类型转换，由于构造函数获取了枚举的子类enums，让遍历更加高效快捷
     * 
     * @param value
     *            数据库中存储的自定义value属性
     * @return value对应的枚举类
     */
    private E valueOf(int value) {
        for (E e : enums) {
            if (e.getCode().equals(value)) {
                return e;
            }
        }

        logger.trace("未知的枚举类型：" + value + ",请核对" + type.getSimpleName());
        return null;
    }
}
