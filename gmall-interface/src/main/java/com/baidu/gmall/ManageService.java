package com.baidu.gmall;

import com.baidu.gmall.bean.*;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-16 19:06
 */
public interface ManageService {

    //http://localhost:8082/getCatalog1

    /**
     * 获取所有的一级分类
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类 id catalog1Id 获取二级分类
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类 id catalog2Id 获取三级分类
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据三级分类 id catalog3Id 获取平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList (String catalog3Id);

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue>
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     *  查询商品集合
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    /**
     *  查询基本销售属性表  就是查询销售属性字典表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     *  保存 商品信息
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId 查询图片列表
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);


    /**
     *  根据 spuId 查询销售属性与销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String  spuId);

    /**
     * 保存 sku 信息
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据 skuId 查询skuInfo信息 进行页面渲染
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据 skuId 查询图片列表
     * @param skuId
     * @return
     */
    List<SkuImage> getSkuImageList(String skuId);

    /**
     * 根据 spuId 查询对应的skuId 中的所有销售属性与销售属性值
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据 spuId 查询该spuId下对应的skuId属性
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 通过平台属性值id  查询获取平台属性与平台属性值
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList);
}
