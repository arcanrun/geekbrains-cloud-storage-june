package NIOServer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ChannelsExample {
    public static void main(String[] args) throws IOException {
        RandomAccessFile raf = new RandomAccessFile("client/src/main/java/nio/file.txt", "rw");
        System.out.println(raf.length());
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(5);
        for (int i = 0; i < 4; i++) {
            buffer.put((byte) ('a' + i));
        }
        buffer.flip();
        channel.write(buffer, 150);
//        buffer.flip();
//        // 97 98 99 100 0
//        //|          ||
////        while (buffer.hasRemaining()) {
////            System.out.print(buffer.get() + ",");
////        }
//        buffer.limit(4);
//        for (int i = 0; i < 4; i++) {
//            buffer.put((byte)('a' + i));
//        }
//        buffer.flip();
//        for (int i = 0; i < buffer.limit(); i++) {
//            System.out.print(buffer.get() + ",");
//        }
    }
}
