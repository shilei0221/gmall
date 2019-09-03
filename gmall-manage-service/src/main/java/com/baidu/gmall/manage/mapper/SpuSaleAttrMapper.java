package com.baidu.gmall.manage.mapper;

import com.baidu.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-19 16:54
 */
public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    /**
     * 根据 spuId查询销售属性以及销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    /**
     * 根据 spuId 查询对应的skuId 中的所有销售属性与销售属性值
     *
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("spuId") String spuId, @Param("skuId") String skuId);
}
