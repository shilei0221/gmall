package com.baidu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.*;
import com.baidu.gmall.manage.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-16 19:11
 */
@Service
public class ManageServiceImpl implements ManageService {


    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper; //注入平台属性mapper

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper; //注入平台属性值 mapper

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper; //注入一级分类 mapper

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper; //注入二级分类 mapper

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper; //注入三级分类 mapper

    /**
     * 获取所有的一级分类
     * @return
     */
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }


    /**
     * 根据一级分类 id catalog1Id 获取二级分类
     * @param catalog1Id
     * @return
     */
    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        //创建一个条件查询类对象 设置条件
        Example example = new Example(BaseCatalog2.class);

        example.createCriteria().andEqualTo("catalog1Id",catalog1Id);

        return baseCatalog2Mapper.selectByExample(example);
    }


    /**
     * 根据二级分类 id catalog2Id 获取三级分类
     * @param catalog2Id
     * @return
     */
    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {

        //根据对象查询 所以创建对象 设置id值
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();

        baseCatalog3.setCatalog2Id(catalog2Id);

        return baseCatalog3Mapper.select(baseCatalog3);
    }


    /**
     * 根据三级分类 id catalog3Id 获取平台属性
     * @param catalog3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {

        //创建对象 将id设置进去 进行查询
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();

        baseAttrInfo.setCatalog3Id(catalog3Id);

        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //判断如果有主键就进行更新 没有主键就进行插入
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {

            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);

        } else {

            //防止主键被赋上一个空字符串 所有设置为 null  然后自动生成
//            baseAttrInfo.setId(null);

            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //如果原来的属性值有值  将原来的属性值全部清空
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        baseAttrValue.setAttrId(baseAttrInfo.getId());

        baseAttrValueMapper.delete(baseAttrValue);

        //重新插入属性值
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {

            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {

                //防止主键被赋上一个空字符串
                attrValue.setId(null);

                //设置 attrId 进属性值表
                attrValue.setAttrId(baseAttrInfo.getId());

                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    /**
     * 根据该attrId 去查找AttrInfo，该对象下 List<BaseAttrValue>
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //创建平台属性对象
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //创建属性值对象
        BaseAttrValue baseAttrValue = new BaseAttrValue();

        baseAttrValue.setAttrId(baseAttrInfo.getId());

        List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.select(baseAttrValue);

        //给属性对象中的属性值集合赋值
        baseAttrInfo.setAttrValueList(baseAttrValues);

        //将属性对象返回
        return baseAttrInfo;

    }
}
