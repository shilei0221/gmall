package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-19 9:55
 *
 * 商品信息表
 */
@Data
public class SpuInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String spuName;

    @Column
    private String description;

    @Column
    private  String catalog3Id;

    @Transient //表中没有的字段 业务需要  使用该注解指定即可
    private List<SpuSaleAttr> spuSaleAttrList;

    @Transient
    private List<SpuImage> spuImageList;

}
