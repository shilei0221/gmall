package com.baidu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.CartInfoService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.CartInfo;
import com.baidu.gmall.bean.SkuInfo;
import com.baidu.gmall.cart.constant.CartConst;
import com.baidu.gmall.cart.mapper.CartInfoMapper;
import com.baidu.gmall.config.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * @author Alei
 * @create 2019-08-28 16:34
 * <p>
 * 购物车实现类
 */
@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil; //注入 redis

    @Reference
    private ManageService manageService; //注入 manageService 获取skuInfo对象

    /**
     * 用户登录状态下 添加购物车
     * 1、先检查该用户的购物车是否存在该商品
     * 2、如果有商品，只要把对应的商品的数量相加 同时更新缓存
     * 3、如果没有该商品，则将对象的商品插入到购物车中，同时插入缓存
     *
     * @param userId
     * @param skuId
     * @param skuNum
     */
    @Override
    public void addToCart(String userId, String skuId, Integer skuNum) {

        CartInfo cartInfo = new CartInfo();

        //将用户id 设置到对象中
        cartInfo.setUserId(userId);

        //将商品id设置到对象中
        cartInfo.setSkuId(skuId);

        //查询数据库中是否存在对应的某个人对应的购物车
        //select * from cartInfo c inner join skuInfo s on c.skuId = s.id where c.userId = ?
        //因为每个人都对应应该有一个购物车 所以传入两个值 获取最终的购物车对象
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

        //判断数据库是否存在该购物车
        if (cartInfoExist != null) {
            //说明存在 进行商品数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);

            //给实时价格赋值  因为实时价格初始值为空 所以我们使用商品的价格给实时价格赋值 进行实时价格初始化
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            //将设置好的购物车对象传入 进行更新数据 数量相加
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        } else {
            //说明数据库中不存在该商品数据 进行保存购物车
            //因为将购物车添加到数据库中 购物车对象为空 所以调用后台管理 获取商品信息 进行对象赋值
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            CartInfo cartInfo1 = new CartInfo();

            /*
             进行将购物车对象赋值 然后添加到数据库
             */
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);

            //添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);

            //如果存在进行数量相加 走if 如果没有则走 else ,走else的时候 cartInfoExist 此对象为空 进行赋值 最后使用共用一个对象
            //共用一个对象 添加到缓存中
            cartInfoExist = cartInfo1;
        }

        //获取 jedis
        Jedis jedis = null;

//        定义key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        try {

            jedis = redisUtil.getJedis();

            //将数据放入 redis 缓存中
            jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoExist));

            //登录状态下购物车需要设置过期时间嘛  如果需要进行设置 可以设置为 30 分钟
            //也可以将购物车的过期时间 设置成与用户登录信息的过期时间一致  用户登录信息失效 购物车失效

            //定义 过期时间的key
            String userKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;

            //获取用户登录的过期时间
            Long ttl = jedis.ttl(userKey);

            //设置购物车的过期时间
            jedis.expire(userKey, ttl.intValue());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 用户登录状态从redis 中获取数据 没有的话 从数据库获取数据
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {

        //从redis中获取购物车数据
        Jedis jedis = null;

        try {

            //获取jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

            //从缓存中获取数据
            //第一种方法  获取缓存中所有的 field 与 value 数据 因为我们不需要 field 商品 id 所以此种方法较麻烦我们不采用
//            jedis.hgetAll(cartKey);

            //第二种方法
            List<String> cartJsons = jedis.hvals(cartKey);

            //判断获取到的数据是否为空 如过为空则说明缓存中没有 从数据库查询
            if (cartJsons != null && cartJsons.size() > 0) {
                //定义一个集合用来封装获取到的购物车数据对象
                List<CartInfo> cartInfoList = new ArrayList<>();

                //遍历获取到的集合数据
                for (String cartJson : cartJsons) {

                    //将遍历出的一个一个数据转为对应的购物车对象
                    CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);

                    //将转换后的购物车对象 放入封装购物车的集合中
                    cartInfoList.add(cartInfo);
                }

                /*
                    给购物车实现一个排序功能
                        我们可以在数据库创建两个字段，创建购物车的时间和更新购物车的时间
                        可以根据更新购物车的时间来排序，实现最新添加的商品默认排在最前面，如果是同一件商品也会根据更新数据排序放在第一位

                        使用自然排序（自定义外部比较器）来实现根据主键排序的功能 使用 lambda表达式简单实现
                 */
                cartInfoList.sort((s1, s2) -> s1.getId().compareTo(s2.getId()));

                //最后将封装后的集合返回
                return cartInfoList;
            } else {
                //说明缓存中没有数据 从数据库中查询对应的商品数据 最后放入缓存中 提高检索效率
                //根据用户id 在数据库进行多变关联查询数据 返回
                List<CartInfo> cartInfoList = loadCartCache(userId);

                return cartInfoList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }


    /**
     * 进行合并购物车
     *  判断该用户登录状态下的购物车与cookie中的购物车是否为空
     *  判断两者的商品id是否相同 如果相同 进行数量相加
     *  如果不同将cookie中的购物车商品添加到数据库中
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {

        //根据用户id 查询该用户是否有购物车
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        //遍历传入的cookie中的数据 获取商品对象 进行合并
        for (CartInfo cartInfoCK : cartListCK) {
            //定义一个标识符 来 代替 else 实现功能
            boolean isMatch = false;
            //遍历从数据库中查询出来的数据

            //判断数据库中是否存在购物车数据 不存在就直接跳过
            if (cartInfoListDB != null && cartInfoListDB.size() > 0) {
                for (CartInfo cartInfoDB : cartInfoListDB) {

                    //将数据库查询出来的数据 与 cookie 中的数据 进行对比 如果商品id 相同 说明是同一件商品 进行数量相加
                    if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())) {

                        //说明商品数量相同 进行累加
                        cartInfoDB.setSkuNum(cartInfoCK.getSkuNum() + cartInfoDB.getSkuNum());

                        //更新数据库中的数量
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);

                        //更改标志符 使其下面判断代码不执行
                        isMatch = true;
                    }
                }

            }

            //判断如果数据库中没有购物车 则直接将cookie中的购物车添加到数据库
            if (!isMatch) {

                //将cookie中的购物车数据 添加到数据库
                cartInfoCK.setUserId(userId);

                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }

        //从新的数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //判断数据库是否存在该商品
        if (cartInfoList != null && cartInfoList.size() > 0) {
            //将数据库中查询出来的最新数据 遍历
            for (CartInfo cartInfo : cartInfoList) {

                //遍历cookie中的数据
                for (CartInfo info : cartListCK) {

                    //判断登录与未登录中的商品是同一个商品吗
                    if (cartInfo.getSkuId().equals(info.getSkuId())) {

                        //将被勾选的商品更新到数据库
                        if ("1".equals(info.getIsChecked())) {
                            //将未登录时的勾选商品赋值给已登录时的商品
                            cartInfo.setIsChecked(info.getIsChecked());

                            //然后在更新redis中的isChecked 状态
                            checkCart(cartInfo.getSkuId(),info.getIsChecked(),userId);
                        }
                    }
                }
            }
        }


        return cartInfoList;
    }

    /**
     * 添加数据到redis中 未登录状态下
     * @param skuId
     * @param userKey
     * @param skuNum
     */
    @Override
    public void addToCartRedis(String skuId, String userKey, int skuNum) {

        /*
            未登录将商品数据放入 redis中
            1、先获取redis中的所有数据
            2、判断数据中的商品是否存在
            3、存在 进行数量相加
            4、不存在 查数据库获取到商品信息 进行拷贝到购物车对象中  放入缓存
         */

        Jedis jedis = null;

        try {
            //获取jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userKey + CartConst.USER_CART_KEY_SUFFIX;

            //获取未登录下的redis中的所有数据  通过key  因为需要通过skuId 获取对应的商品信息
            // 所以此处使用 hgetAll（获取带键值对的集合）方法 不使用 hvals（只获取值的集合）方法
            Map<String, String> map = jedis.hgetAll(cartKey);

            //通过filed 属性 skuid 获取对应的商品值
            String cartInfoJson = map.get(skuId);

            //判断商品值是否为空  如果不为空说明该商品已存在 进行数量相加
            if (StringUtils.isNotEmpty(cartInfoJson)) {

                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);

                cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);

                jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
            } else {

                //说明对应的商品值 不存在 将商品信息获取 设置到对应的购物车中 最后写入redis中
                SkuInfo skuInfo = manageService.getSkuInfo(skuId);

                CartInfo cartInfo = new CartInfo();

                cartInfo.setSkuId(skuId);
                cartInfo.setCartPrice(skuInfo.getPrice());
                cartInfo.setSkuPrice(skuInfo.getPrice());
                cartInfo.setSkuName(skuInfo.getSkuName());
                cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
                cartInfo.setSkuNum(skuNum);

                //将购物车写入redis中
                jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
            }

        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }


    }

    /**
     * 根据key 删除redis中数据
     * @param userKey
     */
    @Override
    public void deleteCartRedis(String userKey) {
        //创建 jedis
        Jedis jedis = null;

        try {

            jedis = redisUtil.getJedis();
//            jedis.del()

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userKey + CartConst.USER_CART_KEY_SUFFIX;

            jedis.del(cartKey);

//            //获取jedis中的所有key对应的值
//            Set<String> hkeys = jedis.hkeys(cartKey);
//
//            //迭代器遍历出对应的值集合
//            for (Iterator<String> iterator = hkeys.iterator(); iterator.hasNext(); ) {
//
//                //获取每次迭代的值
//                String keyStr =  iterator.next();
//
//                //进行删除
//                Long hdel = jedis.hdel(cartKey, keyStr);
//
//                System.out.println(hdel+"*********删除成功");
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 从redis中获取数据显示
     * @param userKey
     * @return
     */
    @Override
    public List<CartInfo> getCartListRedis(String userKey) {

        //定义一个集合 来存储商品信息对象
            List<CartInfo> cartInfoList = new ArrayList<>();

        //创建 jedis
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String cartKey = CartConst.USER_KEY_PREFIX + userKey + CartConst.USER_CART_KEY_SUFFIX;

//            Map<String, String> map = jedis.hgetAll(cartKey);
//            for (String fieldKey : map.keySet()) {
//                String s = map.get(fieldKey);
//                CartInfo cartInfo = JSON.parseObject(s, CartInfo.class);
//                cartInfoList.add(cartInfo);
//            }

            //获取redis中的所有商品信息
            List<String> hvals = jedis.hvals(cartKey);


            //判断如果商品信息 不为空的话
            if (hvals != null && hvals.size() > 0) {

                //遍历商品信息值
                for (String hval : hvals) {


                    //将字符串的值转换为 购物车对象
//                    cartInfoList = JSON.parseArray(hval, CartInfo.class);
                    CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);

                    //将购物车对象放入封装后的集合中
                    cartInfoList.add(cartInfo);
                }
            }

            //进行排序  因为这里数据库没有时间字段 所以我们使用id进行排序
            //cartInfoList.sort((s1,s2)->s1.getId().compareTo(s2.getId()));

            return  cartInfoList;

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return  null;
    }

    /**
     * 更新redis中的数据的勾选状态
     * 同时定义key  将勾选的商品存入redis 中  方便点击结算的时候显示送货清单
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {

        //获取jedis
        Jedis jedis = null;

        try {

            jedis = redisUtil.getJedis();

            //定义key 获取redis中的数据
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

            //根据key  获取对应的商品信息
            String cartJson = jedis.hget(cartKey, skuId);

            //将cartJson 转换为对象
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);

            //将cartInfo中的勾选状态设置为勾选状态（1 为选中  0 为不选中）
            cartInfo.setIsChecked(isChecked);

            //在将购物车对象转换为json串 然后设置到redis
            String cartCheckedJson  = JSON.toJSONString(cartInfo);

            //更新到 redis中
            jedis.hset(cartKey,skuId,cartCheckedJson);

            //定义勾选的购物车 key
            String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

            //判断状态是否为选中状态 如果是选中状态则添加到缓存中 如果不是则删除对应的商品
            if ("1".equals(isChecked)) {
                //添加到redis中
                jedis.hset(cartCheckedKey,skuId,cartCheckedJson);
            } else {
                //删除对应key
                jedis.hdel(cartCheckedKey,skuId);
            }

        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


    /**
     *  获取购物车中的选中商品
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getcartCheckedList(String userId) {

        //获取redis中的key
        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        //获取jedis
        Jedis jedis = null;

        try {

            jedis = redisUtil.getJedis();

            //获取redis中的勾选商品数据
            List<String> cartCheckedList  = jedis.hvals(cartCheckedKey);

            //创建一个集合用来封装最后返回的勾选的购物车集合
            List<CartInfo> cartInfoList = new ArrayList<>();

            //遍历获取的勾选商品数据
            for (String cartJson : cartCheckedList) {

                //将商品数据转换为商品对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);

                //将获取的购物车对象放入集合中
                cartInfoList.add(cartInfo);
            }

            return cartInfoList;

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (jedis!=null) {
                jedis.close();
            }
        }

        return null;
    }


    /**
     * 根据用户id 查询该用户对应的购物车数据
     *
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {

        //自定义sql语句 实现多表关联查询数据库 获取购物车数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        //如果查询的该集合为空 说明该用户没有购物车 返回空
        if (cartInfoList == null && cartInfoList.size() == 0) {
            return null;
        }

        //获取 jedis
        Jedis jedis = null;

        //定义 key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        try {
            jedis = redisUtil.getJedis();

            //第二种方法 定义map集合封装购物车对象 最后一次性添加到redis中
            Map<String, String> map = new HashMap<>();

            for (CartInfo cartInfo : cartInfoList) {

                //将获取到的对象添加到redis缓存中  这样的方式是一个一个往redis中添加
                //第一种方法
//                jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));

                //第二种 使用 map 封装数据
                map.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
            }

            //将 封装数据的map 一次性添加到 redis 中
            jedis.hmset(cartKey, map);

            return cartInfoList;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    /**
     * 用户未登录下存入redis中 改变其勾选状态
     * @param skuId
     * @param isChecked
     * @param userKey
     */
    @Override
    public void checkCartRedis(String skuId, String isChecked, String userKey) {

        //获取未登录下redis中的数据
        List<CartInfo> cartListRedis = getCartListRedis(userKey);

        //判断是否为空
        if (cartListRedis != null && cartListRedis.size() > 0) {

            //遍历集合数据
            for (CartInfo cartListRedi : cartListRedis) {

                //判断是否为同一个商品 是的话设置勾选状态
                if (skuId.equals(cartListRedi.getSkuId())) {
                    //如果是同一件商品 则更改勾选状态
                    cartListRedi.setIsChecked(isChecked);
                }
                //最后在将数据写会到redis中
                Jedis jedis = null;

                try {
                    jedis = redisUtil.getJedis();

                    //定义 key
                    String cartKey = CartConst.USER_KEY_PREFIX + userKey + CartConst.USER_CART_KEY_SUFFIX;

                    //将数据放入redis
                    jedis.hset(cartKey,skuId,JSON.toJSONString(cartListRedi));

                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (jedis != null) {
                        jedis.close();
                    }
                }
            }

        }
    }

    /**
     * 在支付成功之后 同步回调通知中删除勾选时的购物车商品  两个购物车 勾选购物车 与 用户购物车
     * @param userId
     */
    @Override
    public void deleteCheckedCart(String userId) {

        //获取两个购物车对应的key
        //定义 key 用户购物车
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        //获取redis中的key  勾选商品的购物车
        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        //获取 jedis  进行删除

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //删除勾选商品的购物车
            Long del = jedis.del(cartCheckedKey);
            System.out.println("选中的商品购物车删除成功：  " + del);

            //获取用户购物车中的所有数据
            Map<String, String> map = jedis.hgetAll(cartKey);

            //判断获取到的数据是否为空
            if (map != null && map.size() > 0) {

                //遍历集合 获取 field 商品的key
                for (String skuId : map.keySet()) {

                    //获取购物车中的value 商品值
                    String value = map.get(skuId);

                    //将获取到的值转换为对象
                    CartInfo cartInfo = JSON.parseObject(value, CartInfo.class);

                    //进行删除购物车中的商品数据  判断集合中的 选中状态是否为1
                    if ("1".equals(cartInfo.getIsChecked())) {

                        //进行删除数据
                        Long hdel = jedis.hdel(cartKey, skuId);

                        System.out.println("用户购物车中选中商品删除成功：  " + hdel);
                    }
                }
            }


        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if ( jedis != null) {
                jedis.close();
            }
        }

    }
}
