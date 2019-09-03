package com.baidu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.ListService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.SkuImage;
import com.baidu.gmall.bean.SkuInfo;
import com.baidu.gmall.bean.SkuSaleAttrValue;
import com.baidu.gmall.bean.SpuSaleAttr;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-21 16:36
 */
@Controller
public class ItemController {

    //注入ManageService
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;


    /**
     *
     * @param skuId
     * @param request
     * @return
     */
//    @LoginRequire
    @RequestMapping("{skuId}.html")
    public String skuInfoPage(@PathVariable String skuId, HttpServletRequest request) {

        //根据skuId去查询商品信息 在页面进行展示
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //根据 skuId 查询图片列表
        List<SkuImage> skuImageList = manageService.getSkuImageList(skuId);

        //根据 spuId 查询 skuId 的销售属性以及销售属性值集合  因为销售属性里边有销售属性值集合 所以直接查询销售属性就可以获取对应的数据
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //实现用户点击销售属性值的时候根据用户选择的销售属性值找到对应的sku商品然后跳转对应的商品详情页
        //根据spuId值查询其下所有的skuId值的销售属性与销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //遍历查询出来的集合数据  进行数据分割  转拼接成我们要的数据 传递给前端 实现用户点击
        //定义 key
        String key = "";
        //定义存储数据的 map 集合
        Map<String,String> map = new HashMap<>();

        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {

            //最终要封装的数据  {"118 | 120 = 33,119 | 121 = 34.."}


            // 第一次拼接 key="" key = key + 122  key =122
            // 第二次拼接 key=122 key = key + |   key= 122|
            // 第三次拼接 key=122| key = key + 124 key = 122|124
            // 第四次拼接 将 key 放入map 中 map.put(key,skuId); 然后将key 清空
            // 什么时候拼接|
            for (int i = 0; i < skuSaleAttrValueList.size(); i++) {

                //获取 销售属性值
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);

                //如果 key 长度大于零 说明里边有数据 进行拼接 |
                if (key.length() != 0) {
                    key += "|";
                }

                //如果key 中没有数据 进行拼接 销售属性值id
                key += skuSaleAttrValue.getSaleAttrValueId();

                //如果拼接到集合的最后一个就不进行拼接 或 skuId 不相等的时候就不进行拼接
                //当i+1 等于集合长度的时候 说明到集合的最后一个元素了 所以就不进行拼接了
                //当销售属性值中的skuId不等于 下一个 销售属性值中的skuId的时候就不进行拼接了
                //skuSaleAttrValueList.get(i+1).getSkuId()  就等于获取其中的下一个 销售属性值对象 调用其中的skuId
                if ((i+1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())) {

                    //当这两个条件满足一个的时候 将 拼接号的key 放入map 中
                    map.put(key,skuSaleAttrValue.getSkuId());
                    //将key清空 进行下次遍历的时候使用  继续拼接
                    key = "";
                    
                }
            }
            
        }

        //将 map 转换为 json 串
        String svaluesSkuJson  = JSON.toJSONString(map);


        //调用listService 接口中的方法 实现 根据 商品id 在用户点击的时候 实现热度排名
        listService.incrHotScore(skuId);


        //将转换后的 json 串 存入到 域中
        request.setAttribute("svaluesSkuJson",svaluesSkuJson);

        //将查出的销售属性销售属性值 让入域中
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);

        //保存skuInfo数据  提供前端获取
        request.setAttribute("skuInfo",skuInfo);

        //将skuImageList 图片列表保存  提供前端显示
        request.setAttribute("skuImageList",skuImageList);

        return "item";
    }
}
