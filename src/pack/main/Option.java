package pack.main;

import pack.util.ErrorLog;

public class Option {
	private String name, description;

	private Class<? extends Object> classVar;
	private Object defaultValue;
	private Object value;

	Option(String name, String description, Class<? extends Object> type) {
		this.name = name;
		this.description = description;
		this.classVar = type;
	}
	Option(String name, String description, Object defaultValue) {
		this(name, description, defaultValue.getClass());
		this.defaultValue = defaultValue;
	}

	public Class<? extends Object> getType() {
		return this.classVar;
	}
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		return this.description;
	}
	public boolean hasDefaultValue(){
		return this.defaultValue != null;
	}
	public Object getValue() {
		if(this.value != null) {
			return this.value;
		}
		if(this.defaultValue != null) {
			return this.defaultValue;
		}
		ErrorLog.print("Option " + this.name + " has no value");
		return null;
	}
	public Object getDefaultValue() {
		if(this.defaultValue != null) {
			return this.defaultValue;
		}
		ErrorLog.print("Option " + this.name + " has no default value");
		return null;
	}
	void setValue(Object value) {
    	if(!value.getClass().equals(this.classVar)) {
        	ErrorLog.print("Incorrect class " + value.getClass() + " for option " + this.name + ", should be " + this.classVar);
    	}
    	this.value = value;
    }
}
