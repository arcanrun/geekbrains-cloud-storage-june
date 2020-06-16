import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileCommandHandler implements Runnable {

    private Socket socket;
    private IOServer server;
    private boolean isRunning;
    private DataInputStream in;
    private DataOutputStream out;
    private String userName = "user";
    private String serverPath = "C:\\Users\\mlev1219\\IdeaProjects\\geekbrains-cloud-storage-june\\server\\src\\main\\resources\\serverPath";
    private static int cnt = 1;
    private File userDir;
    private String dir;

    public FileCommandHandler(Socket socket, IOServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        isRunning = true;
        userName += cnt;
        cnt++;
        userDir = new File(serverPath + "\\" + userName);
        if (!userDir.exists()) {
            boolean created = userDir.mkdir();
        }
        dir = userDir.getAbsolutePath();
    }

    public void stop() {
        isRunning = false;
        try {
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Client handled!");
        //server side
        //client upload file to server
        try {
            while (isRunning) {
                String command = in.readUTF();
                System.out.println("Client message = " + command);
                if (command.equals("./send")) {
                    System.out.println("Command ./send");
                    String fileName = in.readUTF();
                    System.out.println("fileName: " + fileName);
                    long fileLength = in.readLong();
                    System.out.println("fileLength: " + fileLength);
                    byte [] buffer = new byte[1024];
                    File file = new File(dir + "\\" + fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    for (long i = 0; i <= fileLength / 1024; i++) {
                        int read = in.read(buffer);
                        System.out.println("bytes readed!");
                        fos.write(buffer, 0, read);
                        fos.flush();
                    }
                    fos.close();
                    if (file.length() == fileLength) {
                        System.out.println("file downloaded!");
                        out.writeUTF("File " + fileName + " uploaded!");
                        out.flush();
                    } else {
                        throw new RuntimeException("File downloaded with errors." +
                                " Sended fileLength = " + fileLength + ", actually fileLength = " + file.length());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
