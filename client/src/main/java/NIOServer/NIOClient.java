package NIOServer;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

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

                    while (read != -1){
                        buf.flip();
                        byte firstByte = buf.get();
                        if (firstByte == 11) {
                            while (buf.hasRemaining()) {
                                filesListStr.append((char) buf.get());
                            }
                            String[] listOfFiles = filesListStr.toString().split(",");
                            for (String str : listOfFiles) {
                                listModel.addElement(str.trim());
                            }
                        }

                        if (firstByte == 12) {
                            //download file
                            while (buf.hasRemaining()){
                                System.out.print((char)buf.get());
                            }

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
                buf.clear();
                buf.put(selectedFile.getBytes());
                buf.flip();

                try {
                    while (buf.hasRemaining()) {
                        socketChannel.write(buf);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
