package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-19 16:51
 *
 * 销售属性值表
 */
@Data
public class SpuSaleAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column
    String id ;

    @Column
    String spuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrValueName;

    @Transient //该字段表示销售属性值在页面的时候的选中状态
    String isChecked;
}
