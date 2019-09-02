package com.baidu.gmall.bean.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-24 18:13
 *
 * 将返回的结果封装 最终显示 es 中的数据
 */
@Data
public class SkuLsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    //封装用户检索时的数据
    private List<SkuLsInfo> skuLsInfoList;

    //总记录数
    private long total;

    //总页数
    private long totalPages;

    //封装平台属性值id
    private List<String> attrValueIdList;
}
