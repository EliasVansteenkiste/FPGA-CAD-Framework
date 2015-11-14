package interfaces;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import placers.Placer;

import circuit.Circuit;

import visual.PlacementVisualizer;

class PlacerFactory {

    private static Map<String, String> placers = new LinkedHashMap<>();
    static {
        PlacerFactory.placers.put("random", "placers.random.RandomPlacer");

        PlacerFactory.placers.put("wld_sa", "placers.SAPlacer.WLD_SAPlacer");
        PlacerFactory.placers.put("td_sa", "placers.SAPlacer.TD_SAPlacer");

        PlacerFactory.placers.put("wld_ap", "placers.analyticalplacer.WLD_AnalyticalPlacer");
        PlacerFactory.placers.put("td_ap", "placers.analyticalplacer.TD_AnalyticalPlacer");
    }


    private Logger logger;

    public PlacerFactory(Logger logger) {
        this.logger = logger;
    }

    public Set<String> placers() {
        return PlacerFactory.placers.keySet();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Placer> getClass(String placerName) throws IllegalArgumentException, ClassNotFoundException {
        String classPath = PlacerFactory.placers.get(placerName);

        if(classPath == null) {
            throw new IllegalArgumentException("Non-existent placer: " + placerName);
        }

        return (Class<? extends Placer>) Class.forName(classPath);
    }

    private <T extends Placer> Constructor<T> getConstructor(Class<T> placerClass) throws NoSuchMethodException, SecurityException {
        return placerClass.getConstructor(Circuit.class, Options.class, Random.class, Logger.class, PlacementVisualizer.class);
    }


    public Options initOptions(String placerName) {

        Options options = new Options(this.logger);

        try {
            Class<? extends Placer> placerClass = this.getClass(placerName);
            Method initOptions = placerClass.getMethod("initOptions", Options.class);

            initOptions.invoke(null, options);

        } catch(IllegalArgumentException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException error) {
            this.logger.raise(error);
        }

        return options;
    }



    public Placer newPlacer(String placerName, Circuit circuit, Options options, Random random, PlacementVisualizer visualizer) {
        try {
            Class<? extends Placer> placerClass = this.getClass(placerName);
            Constructor<? extends Placer> placerConstructor = this.getConstructor(placerClass);

            return placerConstructor.newInstance(circuit, options, random, this.logger, visualizer);

        } catch(IllegalArgumentException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException error) {
            this.logger.raise(error);
            return null;
        }
    }
}
