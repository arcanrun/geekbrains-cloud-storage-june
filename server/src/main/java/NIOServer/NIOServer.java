package NIOServer;

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
        ssc.register(selector, SelectionKey.OP_ACCEPT, "SERVER");
        serverPath = Paths.get("server", "src", "main", "resources", "serverPath");
        makeFilesList();
    }

    @Override
    public void run() {
        try {
            System.out.println("Server started on port: 8189.");
            Iterator<SelectionKey> iterator;
            // информация о событии
            SelectionKey key;
            while (ssc.isOpen()) {
                // ожидание события
                int eventsCount = selector.select();
                System.out.println("Selected " + eventsCount + " events.");
                iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    //вытаскиваем какое-то событие из списка событий
                    key = iterator.next();
                    // убираем его их списка событий
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
        System.out.println(key.attachment());
        // кто-то подключился, выдергиваем ссылку на сокет канал
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        clientCount++;
        String userName = "user #" + clientCount;
        channel.configureBlocking(false);
        // этого клиента регистрируем на селекторе, но SelectionKey.OP_READ - т е мы хотим, чтобы Селектор от этого клиента перехватывал события Read
        // т е когда он пришлет что-то что стоило бы почитать
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
                    FileChannel fileChannel = raf.getChannel();
                    byte[] fileNameBytes = file.getFileName().toString().getBytes();

                    buffer.clear();
                    buffer.put((byte) 12);
                    int read = fileChannel.read(buffer);

                    while (read > 0) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            socketChannel.write(buffer);
                        }

                        buffer.clear();
                        System.out.println(read);
                        buffer.put((byte) 12);
                        read = fileChannel.read(buffer);
                    }
                    fileChannel.close();
                    System.out.println(read);

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
