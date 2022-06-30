package javaxt.utils.src;
import javaxt.json.*;

public class Property implements Member {
    private String name;
    
    public String getName(){
        return name;
    }
    
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.set("name", name);
        return json;
    }
}