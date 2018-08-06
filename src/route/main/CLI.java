package route.main;

public class CLI {

    public static void main(String[] args) {
        Logger logger = new Logger();
        Main main = new Main(logger, args);
    }
}