package com.baidu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.ListService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.*;
import com.baidu.gmall.bean.dto.SkuLsInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-16 18:49
 */
@RestController
@CrossOrigin
public class ManageController {

    /*
        @RequestParam 注解可以获取路径中的参数名  如果不指定参数必须与路径参数名一致  指定的话可以随意
     */
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService; //注入list业务层

    /**
     * http://localhost:8082/getCatalog1
     *
     *  查询所有的一级分类 进行显示
     *
     * @return
     */
    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1() {

        return manageService.getCatalog1();
    }

    /**
     * http://localhost:8082/getCatalog2?catalog1Id=2
     *
     * 根据一级分类 id 查询二级分类信息
     * @return
     */
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        return manageService.getCatalog2(catalog1Id);
    }

    /**
     * http://localhost:8082/getCatalog3?catalog2Id=3
     *
     *
     * 根据二级分类 id 查询三级分类信息
     * @param catalog2Id
     * @return
     */
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(@RequestParam("catalog2Id")String catalog2Id) {

        return manageService.getCatalog3(catalog2Id);
    }


    /**
     * http://localhost:8082/attrInfoList?catalog3Id=61
     *
     * 根据三级分类 id 获取平台属性值
     * @return
     */
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> getAttrInfo(@RequestParam("catalog3Id") String catalog3Id) {

        return manageService.getAttrInfoList(catalog3Id);
    }

    /**
     * http://localhost:8082/saveAttrInfo
     *
     * 保存平台属性
     */
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {

        //进行判断 避免空指针
        if (baseAttrInfo != null) {

            manageService.saveAttrInfo(baseAttrInfo);
        }

    }

    /**
     * http://localhost:8082/getAttrValueList?attrId=101
     *
     * 根据平台属性值id 查询平台属性  下面的平台属性值
     * @return
     */
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId) {

        //调用接口方法查询平台属性对象
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);

        //将平台属性对象中的平台属性值返回 进行显示
        return baseAttrInfo.getAttrValueList();
    }

    /**
     *  保存数据到es中
     * @param skuId
     * @return
     */
    @RequestMapping("onSale")
    @ResponseBody
    public String onSale(String skuId) {

        //调用方法获取 skuInfo 对象 用作拷贝到与前台显示对应的类中
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //创建保存到es中的对象 因为是空对象 所以需要进行赋值 对象对拷
        SkuLsInfo skuLsInfo = new SkuLsInfo();

        //进行属性对拷
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        //调用list层方法 将数据保存到es中
        listService.saveSkuLsInfo(skuLsInfo);

        return "OK";
    }
}
