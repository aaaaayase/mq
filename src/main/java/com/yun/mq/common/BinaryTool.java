package com.yun.mq.common;

import java.io.*;

/**
 * @author yun
 * @date 2024/10/20 15:48
 * @desciption: 对象的序列化/反序列化
 */

// 除了message 其它对象也可以通过下面的方法完成序列化以及反序列化
// 前提是类需要去实现一个Serializable接口 不然会报异常（java标准库）
public class BinaryTool {

    // 对象转为字节数组
    public static byte[] toBytes(Object object) throws IOException {

        // 这个流对象相当于一个变长的字节数组
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                // 变长数组包含在这个对象中 因此object对象直接被写入到变长数组当中了
                objectOutputStream.writeObject(object);

            }
            // 转为数组
            return byteArrayOutputStream.toByteArray();
        }
    }

    // 数组转为对象
    public static Object fromBytes(byte[] data) throws IOException, ClassNotFoundException {
        Object object = null;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                // 这里的readObject直接从data数组中读取数据完成反序列化
                object = objectInputStream.readObject();
            }
        }
        return object;
    }

}
