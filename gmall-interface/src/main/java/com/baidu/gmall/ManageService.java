package com.baidu.gmall;

import com.baidu.gmall.bean.BaseAttrInfo;
import com.baidu.gmall.bean.BaseCatalog1;
import com.baidu.gmall.bean.BaseCatalog2;
import com.baidu.gmall.bean.BaseCatalog3;

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
}
