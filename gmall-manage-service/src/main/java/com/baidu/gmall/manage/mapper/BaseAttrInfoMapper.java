package com.baidu.gmall.manage.mapper;

import com.baidu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-16 19:03
 */
public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    /**
     * 根据三级分类id 查询平台属性以及平台属性值 进行显示
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /**
     * 通过 多个平台属性值 id  查询平台属性以及平台属性值
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);

    /**
     * 通过平台属性值id 查询平台属性以及平台属性值
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> getAttrInfoListById(@Param("valueIds") List<String> valueIds);
}
