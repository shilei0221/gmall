package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-19 16:42
 *
 * 销售属性字典表 bean
 */
@Data
public class BaseSaleAttr implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column
    String id;

    @Column
    String name;
}
