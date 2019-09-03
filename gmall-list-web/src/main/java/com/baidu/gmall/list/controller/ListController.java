package com.baidu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.ListService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.BaseAttrInfo;
import com.baidu.gmall.bean.BaseAttrValue;
import com.baidu.gmall.bean.dto.SkuLsInfo;
import com.baidu.gmall.bean.dto.SkuLsParams;
import com.baidu.gmall.bean.dto.SkuLsResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-24 20:11
 */
@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")

    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {

        //在查询之前设置当前页显示id数据
        skuLsParams.setPageSize(2);

        SkuLsResult skuLsResult = listService.search(skuLsParams);

        //获取skuLsInfo 放入域中 前台渲染
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        //因为要显示平台属性  与 平台属性值给用户选择进行过滤  所以将两个属性值表关联出来 进行显示
        //BaseAttrInfo  BaseAttrValue 两张表
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();

        //调用manage业务层中获取平台属性的集合方法  通过平台属性值id 进行多表关联查询平台属性与属性值的数据显示到页面

        //第一种根据valueId 查询出的结果
//        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(attrValueIdList);

        List<BaseAttrInfo> baseAttrInfoList = null;

        //第二种根据三级分类与valueId 查询
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            baseAttrInfoList = manageService.getAttrInfoList(skuLsParams.getCatalog3Id());
        } else {
            baseAttrInfoList = manageService.getAttrInfoList(attrValueIdList);
        }

        //创建一个平台属性值集合  用来封装最新的url 平台属性值对象
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        //构建一个最新的url 路径     // 已选的属性值列表
        String urlParam = makeUrlParam(skuLsParams);

        /*
            功能实现：当用户点击某一个平台属性值id 进行过滤的时候，要将过滤条件拼接到url中然后重新请求一次控制器进行展示，同时应该将点击
                    点平台属性值进行移除
                 因为循环遍历集合不可以进行删除操作，所以我们使用迭代器进行遍历，调用迭代器中的删除方法进行删除集合中的数据
         */

        if (baseAttrInfoList != null && baseAttrInfoList.size() > 0) {


            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {

                //迭代器指针下移得到平台属性对象
                BaseAttrInfo baseAttrInfo = iterator.next();

                //通过平台属性对象获取平台属性值对象
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

                //进行遍历平台属性值集合
                for (BaseAttrValue baseAttrValue : attrValueList) {

                    //判断用户点击的数据中是否存在平台属性值id
                    if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

                        //如果不为空 进行遍历该数组
                        for (String valueId : skuLsParams.getValueId()) {

                            //将选中的平台属性值id 与 查询结果中的属性值id 进行比较 是否相同 相同则说明在进行过滤 进行移除
                            if (valueId.equals(baseAttrValue.getId())) {
                                //调用迭代器中的方法进行移除集合中的数据
                                iterator.remove();

                                //构造面包屑列表   设置面包屑的key name值 （屏幕数据:5G）
                                BaseAttrValue attrValue = new BaseAttrValue();

                                //设置面包屑的key  name、 值
                                attrValue.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());

                                //去除重复数据  重构urlParam 根据valueID 判断用户是否移除过滤条件 重新进行拼接url
                                //如果是移除过滤属性，则判断选择的valueId 是否一致，一致则跳过拼接valueId 进行下一次循环
                                //这样最终的url会没有用户选择的过滤条件 最后请求一次控制器移除面包屑
                                String makeUrlParam = makeUrlParam(skuLsParams, valueId);

                                //将最新拼接的url 设置到 封装数据的平台属性值对象中
                                attrValue.setUrlParam(makeUrlParam);

                                //将最终封装后的 平台属性对象数据 放入集合中
                                baseAttrValueList.add(attrValue);
                            }
                        }
                    }
                }

            }
        }

        //因为当前页显示的条数应该在获取对象时就进行设置 如果在最后设置的话 那就已经查询出来的结果 再去设置 就起不到作用 所以在查询前进行赋值
//        skuLsParams.setPageSize(2);

        //分页
        //将总页数让入域中
        request.setAttribute("totalPages",skuLsResult.getTotalPages());

        //将当前页放入域中
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        //将最终封装的平台属性值集合 放入域中
        request.setAttribute("baseAttrValueList", baseAttrValueList);

        //因为
        request.setAttribute("keyword", skuLsParams.getKeyword());

        //将最新的url 放入 域中
        request.setAttribute("urlParam", urlParam);

        //将平台属性集合放入域中 在前端获取
        request.setAttribute("baseAttrInfoList", baseAttrInfoList);

        //将封装好的用户输入的数据放入域中 最后前端获取显示
        request.setAttribute("skuLsInfoList", skuLsInfoList);


        return "list";
    }

    /**
     * 拼接最新的url 路径 用于点击平台属性值过滤内容
     *
     * @param skuLsParams
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) {
        //定义一个空的字符串 存放url
        String urlParam = "";

        //判断用户是否使用关键字进行检索
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }

        //判断用户是否根据三级分类id检索
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            //判断之前的url是否为空 不为空则是拼接 进行拼接
            if (urlParam.length() > 0) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        //判断用户是否使用平台属性进行过滤
        //构建属性参数
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            //因为valueId 是数组所以遍历获取其中的值 因为每次只能点击一个 所以获取第一个就可以
            for (String valueId : skuLsParams.getValueId()) {

                if (excludeValueIds != null && excludeValueIds.length > 0) {

                    //因为 excludeValueIds 是可变形参 是一个数组  每次用户只能点击一个属性值 所以直接获取第一个数据就可以
                    String excludeValueId = excludeValueIds[0];

                    //判断用户选择的面包屑 属性值 是否等于 url 路径中的valueId 值 如果等于就不进行拼接valueId 直接跳出当前循环
                    if (excludeValueId.equals(valueId)) {
                        //跳出当前循环 后面的参数则不会继续追加【后续代码不执行】
                        //不能写 break 如果写 break return 直接中断此方法 那样的话 其他条件也无法实现 所以只能跳出当前循环
                        continue;
                    }
                }

                //判断url是否为空 不为空继续拼接
                if (urlParam.length() > 0) {

                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }
        return urlParam;
    }
}
