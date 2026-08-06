import java.io.FileInputStream;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("PID: " + ProcessHandle.current().pid());

        System.out.println("Esperando 20 segundos...");
        Thread.sleep(20000);

        FileInputStream readme = new FileInputStream("README.md");

        System.out.println("README abierto.");
        Thread.sleep(20000);

        FileInputStream config = new FileInputStream("config.txt");

        System.out.println("Config abierto.");
        Thread.sleep(20000);

        readme.close();

        System.out.println("README cerrado.");
        Thread.sleep(20000);

        config.close();

        System.out.println("Config cerrado.");
        Thread.sleep(20000);

        System.out.println("Fin.");
    }
}
