package NIOServer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

public class NIOExamples {
    public static void main(String[] args) throws IOException {
        Path path = Paths.get("./", "client", "src", "main", "java", "NIOServer", "file.txt");
        for (Path value : path) {
            System.out.println(value);
        }
        System.out.println(path.toAbsolutePath());
        System.out.println(Files.exists(path));
        // 777
        // 111 111 111
        // rwe rwe rwe
        FileAttribute<?> attrs = PosixFilePermissions
                .asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));
        if (!Files.exists(path)) {
            Files.createFile(path, attrs);
        }
        System.out.println(Files.exists(path));
        Files.copy(path, Paths.get(path.getParent().toString(), "new_file.txt"), StandardCopyOption.REPLACE_EXISTING);
        //FileInputStream fis = new FileInputStream(new File("C:\\Users\\mlev1219\\IdeaProjects\\geekbrains-cloud-storage-june\\client\\src\\main\\java\\NIO\\new_file.txt"));
        //Files.copy(fis, Paths.get(path.getParent().toString(), "is_file.txt"));
        Files.write(Paths.get(path.getParent().toString(), "is_file.txt"), "Hello".getBytes(), StandardOpenOption.APPEND);

    }
}
