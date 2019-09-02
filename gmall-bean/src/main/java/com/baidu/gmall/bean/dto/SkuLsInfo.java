package com.baidu.gmall.bean.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-24 16:28
 *
 * 用于封装前台传入的数据 最终保存到 es中
 */
@Data
public class SkuLsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private BigDecimal price;

    private String skuName;

    private String catalog3Id;

    private String skuDefaultImg;

    //热度排名  默认为零  可以根据热度进行数据排名
    private Long hotScore = 0L;

    private List<SkuLsAttrValue> skuAttrValueList;
}
