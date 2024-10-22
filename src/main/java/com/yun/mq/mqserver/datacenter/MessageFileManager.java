package com.yun.mq.mqserver.datacenter;

import com.yun.mq.common.BinaryTool;
import com.yun.mq.common.MqException;
import com.yun.mq.mqserver.core.MSGQueue;
import com.yun.mq.mqserver.core.Message;

import java.io.*;
import java.util.LinkedList;
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

    // 将消息写入文件
    private void sendMessage(MSGQueue queue, Message message) throws MqException, IOException {
        // 1. 验证要写入的文件是否存在
        if (!checkFilesExists(queue.getName())) {
            throw new MqException("[MessageFileManager] 队列对应的文件不存在！ queueName=" + queue.getName());
        }

        // 2. 将message对象进行序列化 转为二进制字节数组
        byte[] messageBinary = BinaryTool.toBytes(message);

        synchronized (queue) {
            // 3. 写入文件之前先更新message中的offset属性值
            File queueDataFile = new File(getQueueDataPath(queue.getName()));
            message.setOffsetBeg(queueDataFile.length() + 4);
            message.setOffsetEnd(queueDataFile.length() + 4 + messageBinary.length);

            // 4. 写入文件
            try (OutputStream outputStream = new FileOutputStream(queueDataFile, true)) {
                // 这里使用到DataOutputStream类是因为要写入4个字节长度的消息长度 如果直接outputstream调用write写入就是一个字节
                try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
                    // 写入消息长度
                    dataOutputStream.writeInt(messageBinary.length);
                    // 写入消息本体
                    dataOutputStream.write(messageBinary);
                }
            }

            // 5. 更新消息统计文件
            Stat stat = readStat(queue.getName());
            stat.totalCount += 1;
            stat.validCount += 1;
            writeStat(queue.getName(), stat);
        }
    }

    // 删除消息
    private void deleteMessage(MSGQueue queue, Message message) throws IOException, ClassNotFoundException {
        synchronized (queue) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(getQueueStatPath(queue.getName()), "rw")) {
                // 1. 从文件中将相应message读取出来
                byte[] bufferSrc = new byte[(int) (message.getOffsetEnd() - message.getOffsetBeg())];
                randomAccessFile.seek(message.getOffsetBeg());
                randomAccessFile.read(bufferSrc);
                Message diskMessage = (Message) BinaryTool.fromBytes(bufferSrc);

                // 2. 修改diskMessage中的isValid字段
                // 此处无需给参数中的message中的isValid也进行设置 因为在我们删硬盘中的message的情况时内存中的message也是要删的 所以无需再去修改它的属性了
                diskMessage.setIsValid((byte) 0x00);

                // 3. 重新将对象写回文件
                randomAccessFile.seek(message.getOffsetBeg());
                byte[] bufferDest = BinaryTool.toBytes(diskMessage);
                randomAccessFile.write(bufferDest);
            }

            // 更新统计文件
            Stat stat = readStat(queue.getName());
            if (stat.validCount > 0) {
                stat.validCount -= 1;
            }
            writeStat(queue.getName(), stat);
        }

    }

    // 加载对应队列中的消息到内存中 准确来说就是链表
    // 这里使用链表是为了方便操作
    // 这里的操作不需要加锁 因为这是在启动服务器时执行的操作 所以此时还没有用户请求不需要考虑冲突的情况
    public LinkedList<Message> LoadAllMessageFromQueue(String queueName) throws IOException, MqException, ClassNotFoundException {
        LinkedList<Message> messages = new LinkedList<>();
        try (InputStream inputStream = new FileInputStream(getQueueDataPath(queueName))) {
            try (DataInputStream dataOutputStream = new DataInputStream(inputStream)) {
                // 设置变量记录文件中的光标
                long currentOffset = 0;
                // 使用循环把文件中的消息全部读出来转为内存中的对象
                while (true) {
                    // 1. 先读取消息长度 这一步可能会读到文件尾 读到文件尾会抛出EOFException异常
                    int messageSize = dataOutputStream.readInt();
                    // 2. 读出的消息先装到数组
                    byte[] buffer = new byte[messageSize];
                    int actualSize = dataOutputStream.read(buffer);
                    //判断记录的消息长度是否和读出来的长度相等 不相等则抛异常
                    if (actualSize != messageSize) {
                        throw new MqException("[MessageFileManager] 文件格式错误！ queueName=" + queueName);
                    }
                    // 3. 反序列化为对象
                    Message message = (Message) BinaryTool.fromBytes(buffer);
                    // 4. 验证对象是否有效
                    if (message.getIsValid() != 0x01) {
                        // 即使对象无效 读取后文件中光标也移动了
                        currentOffset += (4 + messageSize);
                        continue;
                    }
                    // 5. 计算起始offset以及终止offset
                    message.setOffsetBeg(currentOffset + 4);
                    message.setOffsetEnd(currentOffset + 4 + messageSize);
                    currentOffset += (4 + messageSize);
                    messages.add(message);
                }
            }
        } catch (EOFException e) {
            // 这里的catch不是为了去捕捉异常 而是这里的流对象读到文件尾会抛出异常 因此抓到相应异常就代表文件读取已经结束了
            System.out.println("[MessageFileManager] 恢复Message数据完成！");
        }
        return messages;
    }

}
