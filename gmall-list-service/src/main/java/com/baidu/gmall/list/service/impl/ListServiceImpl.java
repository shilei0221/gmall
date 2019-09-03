package com.baidu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.baidu.gmall.ListService;
import com.baidu.gmall.bean.dto.SkuLsInfo;
import com.baidu.gmall.bean.dto.SkuLsParams;
import com.baidu.gmall.bean.dto.SkuLsResult;
import com.baidu.gmall.config.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-24 16:35
 */
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient; //注入 es 连接客户端

    @Autowired
    private RedisUtil redisUtil; //因为需要使用redis 所以引入 redis

    public static final String ES_INDEX = "gmall"; //定义索引库名

    public static final String ES_TYPE = "SkuInfo"; //定义数据表

    /**
     *  保存 数据到es中
     * @param skuLsInfo
     */
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {


        //如果是查询 使用 Search.Builder 为查询  使用Index.Builder 为保存数据到es索引库中

        //保存数据
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            //调用客户端执行 保存操作
            DocumentResult execute = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询 es 中的数据 最后进行显示
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {

        /*
            实现步骤：
                1. 定义 dsl 语句
                2、 定义要执行的动作
                3、jestClient 执行动作
                4、获取返回的结果
         */
        //定义 dsl 语句 因为语句内容太多，造成代码的复杂性 所以封装一个方法实现 dsl 语句
        String query = makeQueryStringForSearch(skuLsParams);

        //定义要执行的动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        //执行操作
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //获取返回的结果 为了使代码简介 封装方法实现
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);

        return skuLsResult;
    }

    /**
     * 根据某个商品di去 更新 商品的热度 进行排名操作
     * @param skuId
     */
    @Override
    public void incrHotScore(String skuId) {

        Jedis jedis = null;

        try {
            //获取jedis
            jedis = redisUtil.getJedis();

            //定义当热度达到一定值的时候更新一次es
            int timesToEs = 10;

            //因为我们做的是排序 所以使用 Zset  它有一个评分的属性 可以将评分的值设置自增 进行排序
            Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);

            if (hotScore % timesToEs == 0) {
                //调用更新es的方法进行更新es的热度排名
                updateHotScore(skuId,Math.round(hotScore));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 用来更新 es 中热度排名的数据
     * @param skuId
     * @param hotScore
     */
    private void updateHotScore(String skuId, long hotScore) {

        /*
            1. 定义 dsl 语句
            2. 定义执行的动作
            3. 使用 jestClient 执行动作
            因为没有返回值 所以不需要第四步的 获取返回结果
         */
        String updEs = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":"+hotScore+"\n" +
                "  }\n" +
                "}";

        //定义执行的动作
        Update build = new Update.Builder(updEs).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 实现将返回的结果转换为对象形式返回给前端
     * @param skuLsParams
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {

        //创建结果集对象
        SkuLsResult skuLsResult = new SkuLsResult();

        //封装用户检索时的数据
        //private List<SkuLsInfo> skuLsInfoList;
        //设置 用户输入的数据集合  创建封装用户输入数据的集合
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();

        //      skuLsInfoArrayList 集合数据应该从es 中查询得到！ 因为es中获取的是集合
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);

        //进行遍历数据集合
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {

            //获取用户数据对象 skuLsInfo
            SkuLsInfo skuLsInfo = hit.source;

//            skuLsInfo 中的 skuName 并不是高亮字段，所以获取高亮字段 将之前的替换
            Map<String, List<String>> highlight = hit.highlight;
            
            //判读获取的高亮字段是否为空
            if (highlight != null && highlight.size() > 0) {
                
                //获取高效字段对应的值 因为是map 直接get获取
                List<String> list = highlight.get("skuName");
                
                //获取到list中的第一个值  因为高亮显示的名字只有一个
                String skuNameHI = list.get(0);

                //替换之前的skuName
                skuLsInfo.setSkuName(skuNameHI);
            }

            //将获取到的对象放入集合封装
            skuLsInfoList.add(skuLsInfo);

        }

        //将获取到的用户数据集合设置到结果集中
        skuLsResult.setSkuLsInfoList(skuLsInfoList);


        //总记录数
        //private long total;
        //设置总记录数
        skuLsResult.setTotal(searchResult.getTotal());

        //总页数
        //private long totalPages;
        //设置总页数
        //第一种方法 使用三元运算符  判断总记录数取模显示条数是否为0，是则整除，不是则整除在加一，余一页
//        skuLsResult.setTotalPages(searchResult.getTotal() % skuLsParams.getPageSize() == 0 ? searchResult.getTotal() / skuLsParams.getPageSize() : searchResult.getTotal() / skuLsParams.getPageSize() + 1);

        //第二种方法 工作中最常用的一种 总记录数 加 总条数减去一（因为索引从零开始） 整除 条数
        skuLsResult.setTotalPages((searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize());

        //封装平台属性值id
        //private List<String> attrValueIdList;
        //因为平台属性值是个集合 所以声明一个集合来封装平台属性值Id
        List<String> skuAttrValueList = new ArrayList<>();

        //因为平台属性值id在聚合中 所以获取聚合对象获取定义的分组名 获取值
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");

        //获取存放平台属性值id的桶对象
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();

        //遍历桶集合  获取平台属性值 id
        for (TermsAggregation.Entry bucket : buckets) {

            //获取平台属性值id
            String valueId = bucket.getKey();

            //放入对应的集合中 封装
            skuAttrValueList.add(valueId);
        }

        //将平台属性值集合放入最终集合中 封装
        skuLsResult.setAttrValueIdList(skuAttrValueList);

        return skuLsResult;
    }

    /**
     * 定义查询的 dsl 语句  动态生成 dsl 语句
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {

       //创建查询的 builder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //创建查询的bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

       //判断用户是否输入关键字  skuLsParams用户输入的条件   // 判断keyword = skuName
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {

            //说明用户使用的关键字查询
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());

            //将关键字让入bool中
            boolQueryBuilder.must(matchQueryBuilder);

            //因为 关键字要分词 高亮显示  所以只能在此if判断中进行 高亮显示
            //获取高亮对象
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();

            //设置高亮显示的后缀
            highlighter.postTags("</span>");

            //设置高亮前缀
            highlighter.preTags("<span style=color:red>");

            //设置高亮显示的字段 关键字
            highlighter.field("skuName");

            // 将设置好的高亮对象放入查询器
            searchSourceBuilder.highlight(highlighter);
        }

        //判断平台属性值id 是否为空
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

            //因为平台属性值是个数组 循环遍历
            for (String valueId : skuLsParams.getValueId()) {

                //获取平台属性值的term对象
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);

                // filter -- term  将平台属性值id放入查询器中
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //判断三级分类id 是否为空 不为空说明用户点击的三级分类id进行检索  而不是关键字搜索
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {

            //获取三级分类term 对象
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());

            // filter -- term
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //将过滤条件放入查询中 query
        searchSourceBuilder.query(boolQueryBuilder);

        //进行排序  根据热度排序
        searchSourceBuilder.sort("hotScore",SortOrder.DESC);

        //进行分页 from 当前页  size 当前页显示的条数
        //  从第几条数据开始显示
        //     select * from skuInfo limit (pageNo-1)*pageSize ,pageSize
        //      3  , 2  |  0,2  | 2, 2
        searchSourceBuilder.from((skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize());

        searchSourceBuilder.size(skuLsParams.getPageSize());


        //设置聚合
        // 将term 封装到agg ，按照skuAttrValueList.valueId 进行聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");

        //将聚合的对象放入查询器中
        searchSourceBuilder.aggregation(groupby_attr);

        //将查询器转换为字符串返回
        String query = searchSourceBuilder.toString();

        // 动态生成的dsl 语句！
        System.out.println("query:"+query);

        return query;

    }
}
