package com.masget.command.mybatis;

//import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * <b>function:</b> 枚举接口
 * @author hoojo
 * @createDate 2017-3-13 上午9:48:16
 * @file BaseEnumInterface.java
 * @email hoojo_@126.com
 * @version 1.0
 */
//@JsonSerialize(using = EnumJsonSerializer.class)
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface BaseEnumInterface<E extends Enum<?>, T> {

    public T getCode();
    public String getName();
    public String getEnumName();
}
