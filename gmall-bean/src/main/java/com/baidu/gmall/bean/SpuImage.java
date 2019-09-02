package com.baidu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @author Alei
 * @create 2019-08-19 16:52
 *
 * 商品图片实体类
 */
@Data
public class SpuImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column
    @Id
    private String id;
    @Column
    private String spuId;
    @Column
    private String imgName;
    @Column
    private String imgUrl;
}
