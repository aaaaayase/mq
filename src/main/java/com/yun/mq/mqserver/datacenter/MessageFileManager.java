package com.yun.mq.mqserver.datacenter;

import java.io.*;
import java.util.Scanner;

/**
 * @author yun
 * @date 2024/10/20 12:09
 * @desciption: 针对硬盘上的消息进行管理
 */
public class MessageFileManager {

    // 定义内部类来表示队列的统计信息
    // 内部类常用静态内部类 与外部类解耦更充分
    static public class Stat {
        public int totalCount; // 队列中消息总数
        public int validCount; // 队列中的有效消息的数量
    }

    // 这个方法获取指定队列对应的消息文件所在的路径
    private String getQueueDir(String queueName) {
        return "./data/" + queueName;
    }

    // 用于获取指定队列的消息的数据文件路径
    // 其实二进制数据并不适用于使用txt文件表示
    // .bin .bat
    private String getQueueDataPath(String queueName) {
        return getQueueDir(queueName) + "/queue_data.txt";
    }

    // 用于获取指定队列的消息的统计文件路径
    private String getQueueStatPath(String queueName) {
        return getQueueDir(queueName) + "/queue_stat.txt";
    }

    // 读取相应的队列的消息统计文件
    private Stat readStat(String queueName) {
        Stat stat = new Stat();
        try (InputStream inputStream = new FileInputStream(getQueueStatPath(queueName))) {
            Scanner scanner = new Scanner(inputStream);
            stat.totalCount = scanner.nextInt();
            stat.validCount = scanner.nextInt();
            return stat;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // 写入相应队列的消息统计文件
    private void writeStat(String queueName, Stat stat) {
        // 使用OutputStream默认情况下会清空文件内容之后再写入 除非设置append为true
        try (OutputStream outputStream = new FileOutputStream(getQueueStatPath(queueName))) {
            PrintWriter printWriter = new PrintWriter(outputStream);
            printWriter.write(stat.totalCount + "/t" + stat.validCount);
            printWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 创建队列的消息目录以及文件
    private void createQueueFiles(String queueName) throws IOException {
        // 1. 先创建队列的对应的消息目录
        File baseDir = new File(getQueueDir(queueName));
        if (!baseDir.exists()) {
            // 不存在则创建目录
            boolean ok = baseDir.mkdirs();
            if (!ok) {
                throw new IOException("创建目录失败！ baseDir=" + baseDir.getAbsolutePath());
            }
        }

        // 2. 创建队列的消息数据文件
        File queueDataFile = new File(getQueueDataPath(queueName));
        if (!queueDataFile.exists()) {
            boolean ok = queueDataFile.createNewFile();
            if (!ok) {
                throw new IOException("创建数据文件失败！ queueDataFile=" + queueDataFile.getAbsolutePath());
            }
        }

        // 3. 创建队列的消息统计文件
        File queueStatFile = new File(getQueueStatPath(queueName));
        if (!queueStatFile.exists()) {
            boolean ok = queueStatFile.createNewFile();
            if (!ok) {
                throw new IOException("创建统计文件失败！ queueStatFile=" + queueStatFile.getAbsolutePath());
            }
        }

        // 4. 写入消息统计文件的默认值
        Stat stat = new Stat();
        stat.validCount = 0;
        stat.totalCount = 0;
        writeStat(queueName, stat);
    }

    // 删除队列消息的目录以及文件
    private void destroyQueueFiles(String queueName) throws IOException {
        File queueDataFile = new File(getQueueDataPath(queueName));
        boolean ok1 = queueDataFile.delete();
        File queueStatFile = new File(getQueueStatPath(queueName));
        boolean ok2 = queueStatFile.delete();
        File baseDir = new File(getQueueDir(queueName));
        boolean ok3 = baseDir.delete();

        if (ok2 || ok1 || ok3) {
            // 有一个删除失败则算做整体删除失败
            throw new IOException("删除队列的消息目录及文件失败！ baseDir=" + baseDir.getAbsolutePath());
        }
    }

    // 检验队列的消息文件是否存在a
    private boolean checkFilesExists(String queueName) {
        File queueDataFile = new File(getQueueDataPath(queueName));
        if (!queueDataFile.exists()) {
            return false;
        }
        File queueStatFile = new File(getQueueStatPath(queueName));
        if (!queueStatFile.exists()) {
            return false;
        }
        return true;
    }
}
