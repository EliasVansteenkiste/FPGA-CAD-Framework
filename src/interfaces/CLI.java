package interfaces;

import main.Main;

import options.CLIOptions;


public class CLI {

    public static void main(String[] args) {
        CLIOptions cliOptions = new CLIOptions();
        cliOptions.parseArguments(args);

        Main.getInstance().runPlacement();
    }
}
