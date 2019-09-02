package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-19 16:49
 *
 * 销售属性表
 */
@Data
public class SpuSaleAttr implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column
    String id;

    @Column
    String spuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrName;

    @Transient //因为销售属性里边存在多个销售属性值
    List<SpuSaleAttrValue> spuSaleAttrValueList;

}
