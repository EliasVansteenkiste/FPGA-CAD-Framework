package interfaces;

import main.Main;

import options.CLIOptions;
import options.Options;


public class CLI {

    public static void main(String[] args) {
        Logger logger = new Logger();

        CLIOptions cliOptions = new CLIOptions(logger);
        Options options = cliOptions.parseArguments(args);


        Main main = new Main(logger, options);
        main.runPlacement();
    }
}
