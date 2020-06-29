package NIOServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class NIOClient extends JFrame implements ActionListener {
    private static SocketChannel socketChannel;
    private String fileToDownload;
    private FileChannel fileChannel;
    private JButton btnDownload;
    private JList<String> listOfFilesOnServer;
    private ByteBuffer buffer;
    private DefaultListModel<String> listModel;
    private JPanel mainPanel;
    private Path fileSaverPath;


    private static Selector selector;

    public NIOClient() throws IOException {
        buffer = ByteBuffer.allocate(256);
        fileSaverPath = Paths.get("client", "src", "main", "resources", "clientPath");
        prepareGui();
        connect();
        setVisible(true);
    }

    private void prepareGui() {
        setSize(400, 400);
        setLocation(300, 300);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("NIO transfer file demo");

        mainPanel = new JPanel(new BorderLayout());
        btnDownload = new JButton("download");
        btnDownload.addActionListener(this);


        listModel = new DefaultListModel();

        listOfFilesOnServer = new JList<>(listModel);
        listOfFilesOnServer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        mainPanel.add(listOfFilesOnServer, BorderLayout.CENTER);
        mainPanel.add(btnDownload, BorderLayout.SOUTH);
        add(mainPanel);
    }


    private void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("localhost", 8189));
        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT, "CLIENT");


        new Thread(() -> {
            Iterator<SelectionKey> iterator;
            SelectionKey key;
            while (true) {
                try {
                    selector.select();
                    iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        iterator.remove();
                        if (key.isConnectable()) {
                            handleConnect(key);
                        }
                        if (key.isReadable()) {
                            handleRead(key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        buffer.clear();
        int read = socketChannel.read(buffer);
        buffer.flip();
        //signal byte
        int firstByte = buffer.get();
        System.out.println("firstByte: " + firstByte);

        if (firstByte == 11) {
            StringBuilder filesListStr = new StringBuilder();
            while (read > 0) {
                while (buffer.hasRemaining()) {
                    filesListStr.append((char) buffer.get());
                }
                buffer.clear();
                read = socketChannel.read(buffer);
                buffer.flip();
            }
            String[] listOfFiles = filesListStr.toString().split(",");
            for (String str : listOfFiles) {
                listModel.addElement(str.trim());
            }

        }
        if (firstByte == 12) {
            try {
                while (read > 0) {
                    while (buffer.hasRemaining()) {
                        if(buffer.position() != 1){
                            buffer.position(1);
                        }
                        fileChannel.write(buffer);
                    }
                    buffer.clear();
                    read = socketChannel.read(buffer);
                    buffer.flip();

                }


            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel serverSocketChannel = ((SocketChannel) key.channel());
        serverSocketChannel.finishConnect();
        serverSocketChannel.register(selector, SelectionKey.OP_READ, "Server");
        serverSocketChannel.write(ByteBuffer.wrap("Hello from client!".getBytes()));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnDownload) {
            String selectedFile = listOfFilesOnServer.getSelectedValue();

            if (selectedFile != null) {

                try {
                    socketChannel.write(ByteBuffer.wrap(selectedFile.getBytes()));
                    setFileToDownload(selectedFile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }
        }
    }

    public void setFileToDownload(String fileToDownload) {
        this.fileToDownload = fileToDownload;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(fileSaverPath.resolve(this.fileToDownload).toString(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        fileChannel = raf.getChannel();
    }

    public static void main(String[] args) throws IOException {
        new NIOClient();
    }
}
