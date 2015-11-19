package interfaces;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Options {

    public enum Required {TRUE, FALSE};

    private Logger logger;

    private LinkedHashMap<String, Option> options = new LinkedHashMap<String, Option>();
    private int maxNameLength = 0;

    public Options(Logger logger) {
        this.logger = logger;
    }

    public void add(String optionName, String description, Object defaultValue) {
        this.add(new Option(optionName, description, defaultValue));
    }
    public void add(String optionName, String description, Class<? extends Object> classVar) {
        this.add(new Option(optionName, description, classVar));
    }
    public void add(String optionName, String description, Class<? extends Object> classVar, Required required) {
        this.add(new Option(optionName, description, classVar, required));
    }

    private void add(Option option) {
        String optionName = option.getName();
        this.options.put(optionName, option);

        // Remember the longest option length, useful for printing all options
        if(optionName.length() > this.maxNameLength) {
            this.maxNameLength = optionName.length();
        }
    }

    void set(String name, Object value) throws IllegalArgumentException, ClassCastException {
        Option option = this.getOption(name);
        option.setValue(value);
    }

    void set(String name, String value) throws NumberFormatException, IllegalArgumentException {
        Option option = this.getOption(name);
        Class<? extends Object> optionClass = option.getType();

        Object parsedValue = null;
        if(optionClass.equals(String.class)) {
            parsedValue = value;

        } else if(optionClass.equals(Boolean.class)) {
            try {
                parsedValue = (Integer.parseInt(value) > 0);

            } catch(NumberFormatException e) {
                parsedValue = Boolean.parseBoolean(value);
            }

        } else if(optionClass.equals(Integer.class)) {
            parsedValue = Integer.parseInt(value);

        } else if(optionClass.equals(Long.class)) {
            parsedValue = Long.parseLong(value);

        } else if(optionClass.equals(Float.class)) {
            parsedValue = Float.parseFloat(value);

        } else if(optionClass.equals(Double.class)) {
            parsedValue = Double.parseDouble(value);

        } else if(optionClass.equals(File.class)) {
            parsedValue = new File(value);

        } else {
            this.logger.raise(new ClassCastException("Option " + name + " has an unkown class: " + optionClass.getName()));
        }

        option.setValue(parsedValue);
    }


    private Option getOption(String name) throws IllegalArgumentException {
        Option option = this.options.get(name);
        if(option == null) {
            throw new IllegalArgumentException(name + " is not a valid option");
        }

        return option;
    }

    public Object get(String name) throws OptionNotSetException {
        Option option = this.options.get(name);
        if(option == null) {
            throw new OptionNotSetException(name);
        }
        
        return option.getValue();
    }

    public Boolean getBoolean(String name) throws ClassCastException, OptionNotSetException {
        return (Boolean) this.get(name);
    }

    public Integer getInteger(String name) throws ClassCastException, OptionNotSetException {
        return (Integer) this.get(name);
    }

    public Long getLong(String name) throws ClassCastException, OptionNotSetException {
        return (Long) this.get(name);
    }

    public Float getFloat(String name) throws ClassCastException, OptionNotSetException {
        return (Float) this.get(name);
    }

    public Double getDouble(String name) throws ClassCastException, OptionNotSetException {
        return (Double) this.get(name);
    }

    public String getString(String name) throws ClassCastException, OptionNotSetException {
        return (String) this.get(name);
    }

    public File getFile(String name) throws ClassCastException, OptionNotSetException {
        return (File) this.get(name);
    }


    public boolean isRequired(String name) {
        return this.options.get(name).isRequired();
    }

    public String getDescription(String name) {
        return this.options.get(name).getDescription();
    }

    public String getType(String name) {
        return this.options.get(name).getType().getName();
    }


    public Set<String> keySet() {
        return this.options.keySet();
    }
    public Set<Map.Entry<String, Object>> entrySet() {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for(String optionName : this.options.keySet()) {
            values.put(optionName, this.get(optionName));
        }

        return values.entrySet();
    }

    public int getMaxNameLength() {
        return this.maxNameLength;
    }
}
