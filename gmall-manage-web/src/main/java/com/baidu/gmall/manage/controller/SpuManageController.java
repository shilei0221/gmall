package com.baidu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.BaseSaleAttr;
import com.baidu.gmall.bean.SpuImage;
import com.baidu.gmall.bean.SpuInfo;
import com.baidu.gmall.bean.SpuSaleAttr;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-19 9:59
 */
@Controller
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    /**
     * 根据三级分类id 查询商品信息列表
     *
     * http://localhost:8082/spuList？catalog3Id=61
     * @param catalog3Id
     * @return
     */
    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(String catalog3Id) {

        SpuInfo spuInfo = new SpuInfo();

        spuInfo.setCatalog3Id(catalog3Id);

        List<SpuInfo> spuInfoList = manageService.getSpuInfoList(spuInfo);

        return spuInfoList;
    }


    /**
     * http://localhost:8082/baseSaleAttrList
     *
     * 查询所有的销售属性字典表
     *
     * @return
     */
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList() {
        return manageService.getBaseSaleAttrList();
    }


    //http://localhost:8082/saveSpuInfo 保存商品信息
    /**
     * 保存商品信息
     */
    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo) {

        //进行判断 避免空指针
        if (spuInfo != null) {

            manageService.saveSpuInfo(spuInfo);
        }
    }






    /*
        http://localhost:8082/spuImageList?spuId=63

        根据spuId 查询图片列表
     */
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(SpuImage spuImage) {

        return manageService.getSpuImageList(spuImage);
    }

    /**
     *   http://localhost:8082/spuSaleAttrList?spuId=63
     *
     *   根据 spuId 查询销售属性与销售属性值
     */
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {

        return manageService.getSpuSaleAttrList(spuId);
    }
}
