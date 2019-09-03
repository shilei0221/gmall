package com.baidu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.SkuInfo;
import com.baidu.gmall.bean.SpuInfo;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alei
 * @create 2019-08-20 16:56
 */

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;


    /**
     * http://localhost:8082/saveSkuInfo
     *
     *
     */
    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo) {

        if (skuInfo != null) {
            manageService.saveSkuInfo(skuInfo);
        }
    }
}
