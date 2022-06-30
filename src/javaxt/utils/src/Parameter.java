package javaxt.utils.src;
import javaxt.json.JSONObject;

public class Parameter {
    private String name;
    private String type;
    private String description;
    
    public Parameter(String name){
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
    
    public void setType(String type){
        this.type = type;
    }
    
    public String getType(){
        return type;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
    
    public String getDescription(){
        return description;
    }
    
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.set("name", name);
        json.set("type", type);
        json.set("description", description);
        return json;
    }
}
