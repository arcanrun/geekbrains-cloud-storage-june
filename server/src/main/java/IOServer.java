import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;

public class IOServer {

    private ServerSocket srv;
    private ConcurrentLinkedDeque<FileCommandHandler> connections;
    private boolean isRunning;

    public IOServer() throws IOException {
        srv = new ServerSocket(8189);
        System.out.println("Server started");
        connections = new ConcurrentLinkedDeque<>();
        isRunning = true;
        while (isRunning) {
            Socket socket = srv.accept();
            System.out.println("Client with " + socket.getInetAddress() + " connected!");
            FileCommandHandler handler = new FileCommandHandler(socket, this);
            connections.add(handler);
            new Thread(handler).start();
        }
    }

    public static void main(String[] args) throws IOException {
        new IOServer();
    }
}
