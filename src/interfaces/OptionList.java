package interfaces;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OptionList {

    private Logger logger;

    private LinkedHashMap<String, Option> options = new LinkedHashMap<String, Option>();
    private int maxNameLength = 0;

    public OptionList(Logger logger) {
        this.logger = logger;
    }

    public void add(Option option) {
        String optionName = option.getName();
        this.options.put(optionName, option);

        if(optionName.length() > this.maxNameLength) {
            this.maxNameLength = optionName.length();
        }
    }

    void set(String name, String value) {

        Option option = this.options.get(name);

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
            parsedValue = Integer.parseInt(value);

        } else if(optionClass.equals(Float.class)) {
            parsedValue = Float.parseFloat(value);

        } else if(optionClass.equals(Double.class)) {
            parsedValue = Double.parseDouble(value);

        } else if(optionClass.equals(File.class)) {
            parsedValue = new File(value);

        } else {
            Exception error = new ClassCastException("Unknown option value class: " + optionClass.getName());
            this.logger.raise(error);
        }

        option.setValue(parsedValue);
    }

    public Object get(String name) {
        try {
            return this.options.get(name).getValue();

        } catch(OptionNotSetException error) {
            this.logger.raise(error);
            return null;
        }
    }

    public Boolean getBoolean(String name) {
        return (Boolean) this.get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) this.get(name);
    }

    public Long getLong(String name) {
        return (Long) this.get(name);
    }

    public Float getFloat(String name) {
        return (Float) this.get(name);
    }

    public Double getDouble(String name) {
        return (Double) this.get(name);
    }

    public String getString(String name) {
        return (String) this.get(name);
    }

    public File getFile(String name) {
        return (File) this.get(name);
    }


    public boolean isRequired(String name) {
        return this.options.get(name).isRequired();
    }

    public String getDescription(String name) {
        return this.options.get(name).getDescription();
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
