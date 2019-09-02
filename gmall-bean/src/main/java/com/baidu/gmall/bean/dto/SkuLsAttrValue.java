package com.baidu.gmall.bean.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-24 16:31
 *
 * 平台属性值对象 封装前端传入的数据 保存到es中
 */
@Data
public class SkuLsAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String valueId;
}
