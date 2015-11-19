package interfaces;

import interfaces.Options.Required;

class Option {

    private String name, description;
    private Required required;

    private Class<? extends Object> classVar;
    private Object defaultValue;
    private Object value;


    Option(String name, String description, Class<? extends Object> type, Required required) {
        this.name = name;
        this.description = description;
        this.classVar = type;
        this.required = required;
    }
    Option(String name, String description, Class<? extends Object> type) {
        this(name, description, type, Required.TRUE);
    }
    Option(String name, String description, Object defaultValue) {
        this(name, description, defaultValue.getClass(), Required.FALSE);
        this.defaultValue = defaultValue;
    }

    Class<? extends Object> getType() {
        return this.classVar;
    }

    String getName() {
        return this.name;
    }
    String getDescription() {
        return this.description;
    }
    boolean isRequired() {
        return this.required == Required.TRUE;
    }

    Object getDevaultValue() {
        return this.defaultValue;
    }

    void setValue(Object value) {
        if(!value.getClass().equals(this.classVar)) {
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
