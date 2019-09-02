package com.baidu.gmall.bean.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-24 18:10
 *
 * 用户输入的参数 入粒参数 类
 */
@Data
public class SkuLsParams implements Serializable {

    private static final long serialVersionUID = 1L;

    private String keyword;

    private String catalog3Id;

    private String [] valueId;

    private int pageNo = 1;

    private int pageSize = 20;
}
