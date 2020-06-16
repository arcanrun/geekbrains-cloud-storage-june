import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class IOClient extends JFrame {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String clientPath = "C:\\Users\\mlev1219\\IdeaProjects\\geekbrains-cloud-storage-june\\client\\src\\main\\resources\\clientPath";

    public IOClient() throws HeadlessException, IOException {
        setSize(400, 400);
        setLocation(300, 300);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        socket = new Socket("localhost", 8189);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        JTextField txt = new JTextField();
        JButton send = new JButton("send");
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(txt);
        panel.add(send);
        send.addActionListener(action -> {
            String fileName = txt.getText();
            try {
                File file = new File(clientPath + "\\" + fileName);
                if (!file.exists()) {
                    txt.setText("FILE WITH NAME " + fileName + " NOT EXISTS IN DIRECTORY");
                } else {
                    out.writeUTF("./send");
                    out.flush();
                    out.writeUTF(fileName);
                    out.flush();
                    long length = file.length();
                    out.writeLong(length);
                    out.flush();
                    FileInputStream fis = new FileInputStream(file);
                    byte [] buffer = new byte[1024];
                    while (fis.available() > 0) {
                        int read = fis.read(buffer);
                        out.write(buffer, 0, read);
                        out.flush();
                    }
                    String callBack = in.readUTF();
                    txt.setText(callBack);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        add(panel);
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        new IOClient();
    }
}
