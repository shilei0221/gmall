package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-20 17:29
 *
 * 平台属性值
 */
@Data
public class SkuAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column
    String id;

    @Column
    String attrId;

    @Column
    String valueId;

    @Column
    String skuId;
}
