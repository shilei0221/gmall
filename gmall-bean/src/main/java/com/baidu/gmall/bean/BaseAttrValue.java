package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-16 18:57
 *
 * 平台属性值 bean
 */
@Data
public class BaseAttrValue implements Serializable {
    @Id
    @Column
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    @Transient //添加一个字段用于实现面包屑 时点击过滤的属性值时候 将面包屑移除到原来的状态
    private String urlParam;
}

