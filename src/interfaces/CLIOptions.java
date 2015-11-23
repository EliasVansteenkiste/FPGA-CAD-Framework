package interfaces;

import interfaces.Logger.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import util.Pair;


class CLIOptions extends OptionsManager {

    private List<String> args;


    public CLIOptions(Logger logger) {
        super(logger);
    }

    void parseArguments(String[] argArray) {

        // First create a logger
        this.args = this.createLogger(Arrays.asList(argArray));

        // Check if a help argument is provided
        int helpArgIndex = Math.max(this.args.indexOf("-h"), this.args.indexOf("--help"));
        if(helpArgIndex > 0) {
            this.printHelp(Stream.OUT);
            System.exit(0);
        }


        // Get the start positions of the options for the different placers
        List<Pair<String, Integer>> placersAndArgIndexes = this.buildPlacerList();

        Options mainOptions = this.getMainOptions();
        this.parseArguments(0, placersAndArgIndexes.get(0).getSecond(), mainOptions);

        int numPlacers = placersAndArgIndexes.size() - 1;
        for(int placerIndex = 0; placerIndex < numPlacers; placerIndex++) {
            String placerName = placersAndArgIndexes.get(placerIndex + 1).getFirst();
            Options placerOptions = this.getDefaultOptions(placerName);

            int argIndexStart = placersAndArgIndexes.get(placerIndex).getSecond() + 2;
            int argIndexEnd = placersAndArgIndexes.get(placerIndex + 1).getSecond();

            this.parseArguments(
                    argIndexStart,
                    argIndexEnd,
                    placerOptions);

            this.addPlacer(placerName, placerOptions);
        }
    }

    private List<String> createLogger(List<String> argsWithLogger) {
        //TODO: provide an option to log to a file
        return argsWithLogger;
    }

    private List<Pair<String, Integer>> buildPlacerList() {
        List<Pair<String, Integer>> placers = new ArrayList<Pair<String, Integer>>();
        String previousPlacer = "main";

        int numArgs = this.args.size();
        for(int argIndex = 0; argIndex < numArgs; argIndex++) {
            if(this.args.get(argIndex).equals("--placer")) {

                String placer = this.getArgValue(argIndex);
                if(placer == null) {
                    this.printError(argIndex);
                }

                placers.add(new Pair<String, Integer>(previousPlacer, argIndex));
                previousPlacer = placer;
            }
        }

        placers.add(new Pair<String, Integer>(previousPlacer, numArgs));

        return placers;
    }

    private String getArgValue(int argIndex) {
        if(argIndex == this.args.size() - 1) {
            return null;
        }

        if(this.args.get(argIndex + 1).substring(0, 1).equals("-")) {
            return null;
        }

        return this.args.get(argIndex + 1);
    }

    private void parseArguments(int start, int end, Options options) {
        int argIndex = start;

        for(String optionName : options.keySet()) {
            if(options.isRequired(optionName)) {
                String optionValue = this.args.get(argIndex);
                if(optionValue.substring(0, 2).equals("--")) {
                    this.printErrorFormat("Exptected required argument \"%s\" at position %d, got \"%s", optionName, argIndex, optionValue);
                }

                options.set(optionName, optionValue);
                argIndex++;
            }
        }

        while(argIndex < end) {
            String argName = this.args.get(argIndex);
            String optionName = this.argToOption(argName);
            String argValue = this.getArgValue(argIndex);

            // The argument is boolean valued, and should be set to 1
            if(argValue == null) {

                try {
                    options.set(optionName, true);

                } catch(IllegalArgumentException error) {
                    this.printErrorFormat("Invalid argument: \"%s\"", argName);

                } catch(ClassCastException error) {
                    this.printErrorFormat("The argument \"%s\" requires a value", argName);
                }

                argIndex += 1;

            // The argument is differently valued, let the OptionList parse it
            } else {
                try {
                    options.set(optionName, argValue);

                } catch(NumberFormatException error) {
                    String type = options.getType(optionName);
                    this.printErrorFormat("The argument \"%s\" requires a value of type %s, got \"%s\"", argName, type, argValue);

                } catch(IllegalArgumentException error) {
                    this.printErrorFormat("Invalid argument: \"%s\"", argName);
                }

                argIndex += 2;
            }
        }
    }

    private String argToOption(String argName) {
        return argName.substring(2).replace("_", " ");
    }


    private void printErrorFormat(String format, Object... args) {
        this.printError(String.format(format, args));
    }
    private void printError(String message) {
        Stream stream = Stream.ERR;

        this.logger.println(stream, message + "\n");
        this.printHelp(stream);

        System.exit(1);
    }
    private void printError(int argIndex) {

        Stream stream = Stream.ERR;

        String argValue = this.args.get(argIndex);
        this.logger.printf(stream, "Incorrect usage of the option \"%s\" at position %d%n%n", argValue, argIndex);

        this.printHelp(stream);

        System.exit(1);
    }
    private void printHelp(Stream stream) {
        Options mainOptions = this.getMainOptions();

        this.logger.print(stream, "usage: interfaces.CLI");
        this.printRequiredArguments(stream, mainOptions);
        this.logger.println(stream, " [general_options] [--placer placer_name1 [placer_options] [--placer placer_name2 [placer_options] [...]]]");
        this.logger.println(stream);

        this.logger.println(stream, "Attention: the order of arguments matters!");
        this.logger.println(stream, "The --placer option can be specified zero, one or multipler times.");
        this.logger.println(stream, "The chosen placers will be called in the provided order and with the specified options.");
        this.logger.println(stream, "Only the final placement is written to the --output_place_file.");
        this.logger.println(stream);

        this.logger.println(stream, "General options:");
        this.printOptionalArguments(stream, mainOptions);


        for(String placerName : this.placerFactory.placers()) {
            Options placerOptions = this.placerFactory.initOptions(placerName);

            this.logger.println(stream, "--placer " + placerName);
            this.printRequiredArguments(stream, placerOptions);

            this.printOptionalArguments(stream, placerOptions);
        }
    }

    private void printRequiredArguments(Stream stream, Options options) {
        for(String optionName : options.keySet()) {
            if(options.isRequired(optionName)) {
                this.logger.print(stream, " " + optionName.replace(" ", "_"));
            }
        }
    }

    private void printOptionalArguments(Stream stream, Options options) {
        int maxLength = options.getMaxNameLength();

        String format = String.format("  --%%-%ds   %%s%%s%n", maxLength);
        for(String optionName : options.keySet()) {
            if(!options.isRequired(optionName)) {
                String formattedName = optionName.replace(" ", "_");
                String optionDescription = options.getDescription(optionName);

                Object defaultValue = options.get(optionName);
                String defaultString = defaultValue == null ? "" : String.format(" (default: %s)", defaultValue.toString());

                this.logger.printf(stream, format, formattedName, optionDescription, defaultString);
            }
        }

        this.logger.println();
    }
}
