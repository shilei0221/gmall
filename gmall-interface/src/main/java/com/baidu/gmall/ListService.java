package com.baidu.gmall;

import com.baidu.gmall.bean.dto.SkuLsInfo;
import com.baidu.gmall.bean.dto.SkuLsParams;
import com.baidu.gmall.bean.dto.SkuLsResult;

/**
 * @author Alei
 * @create 2019-08-24 16:32
 */
public interface ListService {

    /**
     *  保存 数据到es中
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 查询 es 中的数据 最后进行显示
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 根据某个商品di去 更新 商品的热度 进行排名操作
     * @param skuId
     */
    void incrHotScore(String skuId);
}
