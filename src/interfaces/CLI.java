package interfaces;

import main.Main;

public class CLI {

    public static void main(String[] args) {

        Logger logger = new Logger();
        NewCLIOptions options = new NewCLIOptions(logger);

        options.parseArguments(args);

        Main main = new Main(options);
        main.runPlacement();
    }
}
