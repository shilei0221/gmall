package com.baidu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.*;
import com.baidu.gmall.config.RedisUtil;
import com.baidu.gmall.manage.constant.ManageConst;
import com.baidu.gmall.manage.mapper.*;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
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

    @Autowired
    private SpuInfoMapper spuInfoMapper; //注入商品 mapper

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper; //注入 销售属性字典表 mapper

    @Autowired
    private SpuImageMapper spuImageMapper ; //注入图片列表 mapper

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper; //注入销售属性 mapper

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper; //注入销售属性值 mapper

    @Autowired
    private SkuInfoMapper skuInfoMapper; //注入 sku mapper

    @Autowired
    private  SkuImageMapper skuImageMapper; //注入 sku 的图片列表mapper

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper; //注入销售属性值 mapper

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper; //注入平台属性值 mapper

    @Autowired
    private RedisUtil redisUtil;

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
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//
//        return baseAttrInfoMapper.select(baseAttrInfo);

        //根据三级分类查询 平台属性一级平台属性值
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);


    }

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    @Transactional
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

    /**
     * 查询商品集合
     * @param spuInfo
     * @return
     */
    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }



    /**
     *  查询基本销售属性表  就是查询销售属性字典表
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }


    /**
     *  保存 商品信息
     * @param spuInfo
     *
     *  涉及到四张表 分别是 商品信息 spuInfo  商品图片 spuImage 销售属性 saleAttr 销售属性值 saleAttrValue
     *  分别保存四张表 实现商品保存
     *
     *  因为四张表不可分割 所以 使用事务  要么都成功要么都失败
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        //根据id判断是添加还是修改 商品信息

        if (spuInfo.getId() != null && spuInfo.getId().length() > 0) {
            //修改数据
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        } else {
            //保存数据
            spuInfoMapper.insertSelective(spuInfo);
        }

        //查询图片列表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();

        //判断图片列表是否为空 进行遍历插入
        if (spuImageList != null && spuImageList.size() > 0) {
            //循环遍历
            for (SpuImage spuImage : spuImageList) {
                //因为页面无法传入 spuId 所以我们设置spuId
                spuImage.setSpuId(spuInfo.getId());

                //插入数据库
                spuImageMapper.insertSelective(spuImage);
            }
        }

        //获取销售属性 判断是否有值 进行添加
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            //进行遍历
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //因为spuSaleAttr 中页面无法传入spuId  所以我们设置spuId
                spuSaleAttr.setSpuId(spuInfo.getId());

                //插入销售属性
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //获取销售属性中的销售属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();

                //判断该销售属性值是否有值
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    //进行遍历销售属性值集合
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //因为 销售属性值中的 spuId 无法从页面传入 所以设置 spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());

                        //进行插入
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }

    }


    /**
     * 根据spuId 查询图片列表
     * @param spuImage
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    /**
     *  根据 spuId 查询销售属性与销售属性值
     * @param
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存 sku 信息
     * @param skuInfo
     */
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {

        /*
            因为sku 信息 在页面传递 可以分析出 需要涉及到四张表的数据 所以我们分别对四张表进行保存
               因为 四张表为一个整体 所以我们添加事务 使其要么都成功 要么都失败

               skuInfo : sku 的信息表
               skuImage : 一组sku对应的多张图片
               skuAttrValue : 平台属性值
               skuSaleAttrValue : 销售属性值
         */

        //保存第一张表 skuInfo
        skuInfoMapper.insertSelective(skuInfo);

        //获取图片表进行保存 判断是否为空  保存第二张表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();

        if (skuImageList != null && skuImageList.size() > 0) {

            //遍历图片列表集合
            for (SkuImage skuImage : skuImageList) {

                //设置图片列表中的 skuId
                skuImage.setSkuId(skuInfo.getId());

                //进行添加
                skuImageMapper.insertSelective(skuImage);
            }
        }

        //保存第三张表  获取平台属性值 进行判断是否为空
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();

        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {

            //循环遍历 平台属性值 进行显示
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {

                //设置skuId值
                skuAttrValue.setSkuId(skuInfo.getId());

                //进行保存
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        //保存第四张表  获取销售属性值 进行判断是否为空
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {

            //循环遍历 销售属性值表
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {

                //设置skuId
                skuSaleAttrValue.setSkuId(skuInfo.getId());

                //进行添加
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }


    }

    /**
     * 根据 skuId 查询skuInfo信息 进行页面渲染
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {


        //使用 Redisson 解决分布式锁
//        return getSkuInfoRedisson(skuId);

        //使用 redis 的set 命令 解决分布式锁
        return getSkuInfoRedis(skuId);

        //不使用分布式锁的缓存
//        return getSkuInfoNoRedis(skuId);
    }

    /**
     * 使用 Redisson 解决分布式锁
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(String skuId) {

    // 业务代码
    SkuInfo skuInfo =null;
    RLock lock =null;
    Jedis jedis =null;
    try {

        // 测试redis String
        jedis = redisUtil.getJedis();

        // 定义key
        String userKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
        if (jedis.exists(userKey)){
            // 获取缓存中的数据
            String userJson = jedis.get(userKey);
            if (!StringUtils.isEmpty(userJson)){
                skuInfo = JSON.parseObject(userJson, SkuInfo.class);
                return skuInfo;
            }
        }else {

            // 创建config
            Config config = new Config();
            // redis://192.168.67.220:6379 配置文件中！
            config.useSingleServer().setAddress("redis://192.168.199.134:6379");

            RedissonClient redisson = Redisson.create(config);

            lock = redisson.getLock("my-lock");

            lock.lock();

            // 从数据库查询数据
            skuInfo = getSkuInfoDB(skuId);
            // 将数据放入缓存
            // jedis.set(userKey,JSON.toJSONString(skuInfo));
            jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
            return skuInfo;
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (jedis!=null){
            jedis.close();
        }
        if (lock!=null){
            lock.unlock();
        }

    }
    // 从db走！
    return getSkuInfoDB(skuId);
}

    /**
     * 使用分布式锁  redis 中的 set 命令
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedis(String skuId) {
        Jedis jedis = null;

        SkuInfo skuInfo = null;

        try {
            /*
                1、获取 jedis
                2、判断缓存中是否有数据
                3、如果有 则从缓存获取
                4、没有 走数据库 要加锁 然后放入 redis中
             */
            //获取 jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

            //获取缓存中数据
            String skuJson = jedis.get(skuKey);

            //如果缓存中不存在数据 从数据库中获取 加分布式锁 最后放入缓存中 将锁删除
            if (skuJson == null) {
                System.out.println("缓存中没有数据");

                //分布式锁 set k1 v1 px 10000 nx
                //定义分布式锁的key值 sku:skuId:lock
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;

                //执行命令 创建分布式锁  NX：表示缓存中不存在该key XX：表示数据库存在该key PX：表示单位为毫秒 EX：表示单位为秒
                String lockKey = jedis.set(skuLockKey,"ALEI","NX","PX",ManageConst.SKULOCK_EXPIRE_PX);

                //进行判断 是否获取到锁  因为redis命令执行完毕之后会返回OK 所以判断是否获取到锁
                if ("OK".equals(lockKey)) {
                    System.out.println("已经获取到分布式锁");

                    //调用方法从数据库获取数据
                    skuInfo = getSkuInfoDB(skuId);

                    //将最新的数据转换为 json 字符串 然后让入缓存中 方便下次获取
                    String skuRedisStr = JSON.toJSONString(skuInfo);

                    //将转换后的字符串放入缓存中
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);

                    //之后将锁删除
                    jedis.del(skuLockKey);

                    return skuInfo;
                } else {
                    // 等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            } else {
                //说明数据库有缓存 走缓存获取数据
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);

                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

        //如果 redis 突然宕机了 直接从数据库获取
        return getSkuInfoDB(skuId);
    }

    /**
     * 没有设置分布式锁时候将数据放入缓存中  缓存中没有直接去数据库获取
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoNoRedis(String skuId) {
        //1.获取jedis
        Jedis jedis = null;

        try {

            jedis = redisUtil.getJedis();

            SkuInfo skuInfo = null;

            //定义 key
            String skuInfoKey  = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

            //判断redis中是否存在这个key  如果存在 获取数据，如果不存在走数据库，在将数据放入缓存中
            if (jedis.exists(skuInfoKey)) {
                //获取数据
                String skuInfoJson  = jedis.get(skuInfoKey);

                //判断如果获取到的数据不为空  将获取到的字符串转成对象形式
                if (skuInfoJson != null && skuInfoJson.length() > 0) {

                    //将数据转换成对象
                    skuInfo = JSON.parseObject(skuInfoJson,SkuInfo.class);
                }
            } else {
                //缓存中不存在数据 从数据库获取
                skuInfo = getSkuInfoDB(skuId);

                //将最新的数据放入到缓存中
                String jsonString  = JSON.toJSONString(skuInfo);

                jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
            }


            return skuInfo;

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (jedis != null ) {
                jedis.close();
            }
        }

        return  getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoDB(String skuId){

        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        skuInfo.setSkuImageList(getSkuImageList(skuId));


        // 添加skuAttrValue
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> attrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(attrValueList);
        return skuInfo;
    }

    /**
     * 根据 skuId 查询图片列表
     * @param skuId
     * @return
     */
    @Override
    public List<SkuImage> getSkuImageList(String skuId) {

        SkuImage skuImage = new SkuImage();

        skuImage.setSkuId(skuId);

        return skuImageMapper.select(skuImage);
    }

    /**
     * 根据 spuId 查询对应的skuId 中的所有销售属性与销售属性值
     * @param skuInfo
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getSpuId(),skuInfo.getId());
    }


    /**
     * 根据 spuId 查询该spuId下对应的skuId属性
     * @param spuId
     * @return
     */
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    /**
     * 通过平台属性值id  查询获取平台属性与平台属性值
     * @param attrValueIdList
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList) {
        
        //因为当前平台属性值id 在一个集合中 所以我们将其转换为数组 然后调用StringUtil中的方法将其转换为数组进行分割 然后传入mapper方法中 编写sql语句进行查询
        //如果不想使用这种拼接方法，也可以使用mybatis中的foreach 标签 进行遍历id  最终封装返回结果
//        String attrValueIds  = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ",");

        //最后将转换后的字符串id 传入mapper 中  进行编写sql语句
//        return baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);

        return baseAttrInfoMapper.getAttrInfoListById(attrValueIdList);
    }
}
