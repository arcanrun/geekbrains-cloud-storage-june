package NIOServer;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class NIOClient extends JFrame implements ActionListener {

    private static SocketChannel socketChannel;
    private static InetSocketAddress address;
    private JButton btnDownload;
    private JList<String> listOfFilesOnServer;
    private ByteBuffer buf;
    private DefaultListModel<String> listModel;
    private JPanel mainPanel;
    private Path fileSaverPath;


    public NIOClient() {
        prepareGui();
        connect();
        buf = ByteBuffer.allocate(256);
        fileSaverPath = Paths.get("client", "src", "main", "resources", "clientPath");
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

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    address = new InetSocketAddress("localhost", 8189);
                    socketChannel = SocketChannel.open(address);
                    StringBuilder filesListStr = new StringBuilder();
                    int read = socketChannel.read(buf);
                    while (read != -1) {
                        buf.flip();
                        byte firstByte = buf.get();
                        System.out.println("-->" + firstByte);
                        if (firstByte == 11) {
                            while (buf.hasRemaining()) {
                                filesListStr.append((char) buf.get());
                            }
                            String[] listOfFiles = filesListStr.toString().split(",");
                            for (String str : listOfFiles) {
                                listModel.addElement(str.trim());
                            }
                            break;
                        }

                        if (firstByte == 12) {
                            //download file
                            int fileNameLength = buf.get();
                            StringBuilder fileName = new StringBuilder();
//                            System.out.println(fileNameLength);
//                            for (int i = 0; i < fileNameLength; i++) {
//                                fileName.append((char) buf.get());
//                            }
                            System.out.println("FILE NAME: " + fileName.toString());
                            RandomAccessFile raf = new RandomAccessFile(fileSaverPath + File.separator + fileName, "rw");

                            FileChannel channel = raf.getChannel();

                            while (true)
                                channel.write(buf);


                        }

                        if (firstByte == 13) {
                            //error msg

                        }


                        buf.clear();
                        read = socketChannel.read(buf);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }


    public static void main(String[] args) {

        new NIOClient();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnDownload) {
            String selectedFile = listOfFilesOnServer.getSelectedValue();


            if (selectedFile != null) {
                try {
                    socketChannel.write(ByteBuffer.wrap(selectedFile.getBytes()));
                    ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                    int read = socketChannel.read(byteBuffer);

                    byteBuffer.flip();

                    int firstByte = byteBuffer.get();
                    System.out.println(firstByte);

                    int fileNameLength = byteBuffer.get();
                    StringBuilder fileName = new StringBuilder();
                    System.out.println(fileNameLength);

                    for (int i = 0; i < fileNameLength; i++) {
                        fileName.append((char) byteBuffer.get());
                    }

                    System.out.println("FILE NAME: " + fileName.toString());

                    RandomAccessFile raf = new RandomAccessFile(fileSaverPath.resolve(fileName.toString()).toString(), "rw");
                    FileChannel fileChannel = raf.getChannel();

                    while (read > 0) {
                        while (byteBuffer.hasRemaining()) {
                            fileChannel.write(byteBuffer);
                        }
                        byteBuffer.clear();
                        read = socketChannel.read(byteBuffer);
                        byteBuffer.flip();
                    }
                    fileChannel.close();
                    byteBuffer.clear();


                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        }
    }
}

