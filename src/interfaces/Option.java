package interfaces;

public class Option {

    public enum Required {TRUE, FALSE};


    private String name, description;
    private Required required;

    private Class<? extends Object> type;
    private Object defaultValue;
    private Object value;


    public Option(String name, String description, Class<? extends Object> type, Required required) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
    }
    public Option(String name, String description, Class<? extends Object> type) {
        this(name, description, type, Required.TRUE);
    }
    public Option(String name, String description, Object defaultValue) {
        this(name, description, defaultValue.getClass(), Required.FALSE);
        this.defaultValue = defaultValue;
    }

    public Class<? extends Object> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }
    public String getDescription() {
        return this.description;
    }
    public boolean isRequired() {
        return this.required == Required.TRUE;
    }

    Object getDevaultValue() {
        return this.defaultValue;
    }

    void setValue(Object value) {
        if(!value.getClass().equals(this.type)) {
            throw new ClassCastException("Incorrect class: " + value.getClass());
        }

        this.value = value;
    }

    Object getValue() throws OptionNotSetException {
        if(this.value != null) {
            return this.value;
        }


        if(this.defaultValue != null) {
            return this.defaultValue;
        }

        if(this.required == Required.FALSE) {
            return null;
        }

        throw new OptionNotSetException(this.name);
    }
}
