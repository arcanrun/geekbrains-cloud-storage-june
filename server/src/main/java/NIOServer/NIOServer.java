package NIOServer;

import com.sun.media.jfxmediaimpl.HostUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

public class NIOServer implements Runnable {

    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(256);
    private static int clientCount = 0;
    private static Path serverPath;
    private static List<Path> serverFiles;
    private static byte signalFileTransferByte = 11;

    public NIOServer() throws IOException {
        serverFiles = new ArrayList<>();
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(8189));
        ssc.configureBlocking(false);
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        serverPath = Paths.get("server", "src", "main", "resources", "serverPath");
        makeFilesList();
    }

    @Override
    public void run() {
        try {
            System.out.println("Server started on port: 8189.");
            Iterator<SelectionKey> iterator;
            SelectionKey key;
            while (ssc.isOpen()) {
                int eventsCount = selector.select();
                System.out.println("Selected " + eventsCount + " events.");
                iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        handleAccess(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder message = new StringBuilder();

        int read = 0;
        buffer.rewind();
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            System.out.println(new String(bytes));
            message.append(new String(bytes));
            buffer.rewind();
        }
        if (read < 0) {
            System.out.println(key.attachment() + ": leave!");
            for (SelectionKey send : key.selector().keys()) {
                if (send.channel() instanceof SocketChannel && send.isReadable()) {
                    ((SocketChannel) send.channel()).write(ByteBuffer.wrap((key.attachment() + ": leave!").getBytes()));
                }
            }
            channel.close();
        } else {
            //message.deleteCharAt(message.length()-1);
            System.out.println(key.attachment() + ">>> " + message);

            downloadFile(message.toString(), channel);

        }
//            for (SelectionKey send : key.selector().keys()) {
//                if (send.channel() instanceof SocketChannel && send.isReadable()) {
//                    downloadFile(message.toString(), ((SocketChannel) send.channel()));
//                }
//            }
//        }

    }

    private void handleAccess(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        clientCount++;
        String userName = "user#" + clientCount;
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ, userName);

        buffer.put((byte) 11);
        buffer.put(ByteBuffer.wrap(serverFiles.toString().substring(1, serverFiles.toString().length() - 1).getBytes()));
        buffer.flip();
        channel.write(buffer);


        System.out.println("Client " + userName + " connected from ip: " + channel.getLocalAddress());
    }

    private void makeFilesList() throws IOException {
        Files.walkFileTree(serverPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                serverFiles.add(file.getFileName());
                return FileVisitResult.CONTINUE;

            }
        });
    }

    private void downloadFile(String fileNameToDownload, SocketChannel socketChannel) throws IOException {

        Files.walkFileTree(serverPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals(fileNameToDownload)) {

                    RandomAccessFile raf = new RandomAccessFile(file.toString(), "rw");
                    FileChannel inChanel = raf.getChannel();
                    byte[] fileNameBytes = file.getFileName().toString().getBytes();
                    buffer.clear();
                    buffer.put((byte) 12);
                    buffer.put((byte) fileNameBytes.length);
                    buffer.put(fileNameBytes);
                    inChanel.read(buffer);
                    buffer.flip();
                    socketChannel.write(buffer);

                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;

            }
        });


    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }

}
