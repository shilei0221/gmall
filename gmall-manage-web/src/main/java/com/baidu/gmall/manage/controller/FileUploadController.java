package com.baidu.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Alei
 * @create 2019-08-19 15:16
 */
@CrossOrigin
@RestController
public class FileUploadController {

    //软编码将服务器的 ip 地址的配置到 application.properties 中
    //@Value 注解使用的前提条件是 当前类必须注入到 spring 容器中 否则不能使用
    @Value("${fileServer.url}")
    String fileUrl;


    /**
     * 实现图片上传
     *
     * //    http://localhost:8082/fileUpload 图片上传
     * @param file
     * @return
     */
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) {

        try {
            //定义最终返回的路径
            String imgUrl = fileUrl;

            //判断 file 文件是否为空 进行上传
            if (file != null) {

                //获取 源文件路径下的 tracker.conf 内容
                String configFile = this.getClass().getResource("/tracker.conf").getFile();

                //客户端进行初始化  将 获取到的配置文件内容传入
                ClientGlobal.init(configFile);

                //创建 可以获文件上传地址与信息的客户端 对象
                TrackerClient trackerClient = new TrackerClient();

                //根据客户端获取服务端的连接
                TrackerServer trackerServer = trackerClient.getConnection();

                //根据服务端获取的ip 与 文件信息 找到存储文件的 storage 创建该对象 存储文件
                StorageClient storageClient = new StorageClient(trackerServer,null);

                //获取文件名称   只是获取到文件的名称  没有获取到全路径
                String fileName = file.getOriginalFilename();

                //获取文件后缀名
                String extName = StringUtils.substringAfterLast(fileName,".");

                //fileName 只是一个文件名称 并不是文件全路径 不是文件全路径应该是文件的字节数组
                //使用上传文件的对象 调用上传文件方法 传入文件
                String [] uploadFile = storageClient.upload_file(file.getBytes(),extName,null);


                //遍历文件长度  最终获取返回的 id 与路径 最后拼接到最终的imgUrl 然后返回页面进行显示
                for (int i = 0; i < uploadFile.length; i++) {
                    String path = uploadFile[i];

                    imgUrl += "/" + path;
                }
            }

            //最后的图片全路径地址
            //http://192.168.199.134/group1/M00/00/00/wKjHhl1G8_GAB6MuAAgw6QZ0-ns791.jpg
            return imgUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }
}
