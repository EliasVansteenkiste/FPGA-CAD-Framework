package pack.netlist;

import pack.util.ErrorLog;

public class T{
	private String name;
	private int number;
	private P pin;
	private int type;
	//NULL => 0
	//INPUT => 1
	//OUTPUT => 2
	//CUT => 5

	public T(String name, int number, String type){
		this.name = name;
		this.number= number;
		if(type.equals("INPUT")){
			this.type = 1;
		}else if(type.equals("OUTPUT")){
			this.type = 2;
		}else if(type.equals("CUT")){
			this.type = 5;
		}else if(type.equals("NULL")){
			this.type = 0;
		}else{
			ErrorLog.print("Wrong type for terminal: " + type);
		}
	}
	
	//TRIM AND CLEAN UP
	public void clean_up(){
		this.pin = null;
	}
	
	//GETTERS
	public String get_name(){
		return this.name;
	}
	public int get_number(){
		return this.number;
	}
	
	//TYPE
	public String get_type(){
		if(this.type == 0){
			return "NULL";
		}else if(this.type == 1){
			return "INPUT";
		}else if(this.type == 2){
			return "OUTPUT";
		}else if(this.type == 5){
			return "CUT";
		}else{
			ErrorLog.print("Wrong type");
			return null;
		}
	}
	public boolean is_input_type(){
		return (this.type == 1);
	}
	public boolean is_output_type(){
		return (this.type == 2);
	}
	public boolean is_cut_type(){
		return (this.type == 5);
	}
	
	//PIN
	public void set_pin(P p){
		if(this.pin == null){
			this.pin = p;
		}else{
			ErrorLog.print("Pin " + this.toString() + " already has a net " + p.get_net().toString());
		}
	}
	public boolean has_pin(){
		if(this.pin == null){
			return false;
		}else{
			return true;
		}
	}
	public P get_pin(){
		return this.pin;
	}
	
	//STRING
	public String toString(){
		return this.get_name();
	}
	
	@Override
    public int hashCode() {
		return this.number;
    }
}
