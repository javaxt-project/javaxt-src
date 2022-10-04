package javaxt.utils.src;
import javaxt.json.*;

public class Property implements Member {
    private String name;
    private String description;
    private String type;
    private String defaultValue;
    private boolean isStatic = false;

    public Property(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }

    public void setStatic(boolean isStatic){
        this.isStatic = isStatic;
    }

    public boolean isStatic(){
        return isStatic;
    }

    public void setDefaultValue(String defaultValue){
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue(){
        return defaultValue;
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.set("name", name);
        json.set("type", type);
        json.set("description", description);
        json.set("defaultValue", defaultValue);
        return json;
    }
}