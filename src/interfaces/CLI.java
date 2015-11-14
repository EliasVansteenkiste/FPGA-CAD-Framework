package interfaces;

import main.Main;

public class CLI {

    public static void main(String[] args) {

        Logger logger = new Logger();
        CLIOptions options = new CLIOptions(logger);

        options.parseArguments(args);

        Main main = new Main(options);
        main.runPlacement();
    }
}
