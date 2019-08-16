package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-16 18:57
 *
 * 平台属性 bean
 */
@Data
public class BaseAttrInfo implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY) //获取主键自增注解
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient  //添加数据库不存在的字段 使用该注解
    private List<BaseAttrValue> attrValueList;

}

