package javaxt.utils.src;
import java.util.*;
import javaxt.json.*;

public class Method implements Member {
    private String name;
    private String description;
    private boolean isStatic = false;
    private boolean isPublic = true;
    private String returnType;
    private ArrayList<Parameter> parameters;
    private boolean isDeprecated = false;
    private String deprecationMessage;
    private Integer position;

    public Method(String name){
        this.name = name;
        this.parameters = new ArrayList<>();
    }

    public boolean isPublic(){
        return isPublic;
    }

    public void setPublic(boolean isPublic){
        this.isPublic = isPublic;
    }

    public void setStatic(boolean isStatic){
        this.isStatic = isStatic;
    }

    public boolean isStatic(){
        return isStatic;
    }

    public void setDeprecated(boolean isDeprecated, String message){
        this.isDeprecated = isDeprecated;
        this.deprecationMessage = message;
    }

    public boolean isDeprecated(){
        return isDeprecated;
    }

    public void setReturnType(String returnType){
        this.returnType = returnType;
    }

    public String getReturnType(){
        return returnType;
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

    public void addParameter(Parameter parameter){
        this.parameters.add(parameter);
    }

    public ArrayList<Parameter> getParameters(){
        return parameters;
    }

    public void setPosition(Integer position){
        this.position = position;
    }

    public Integer getPosition(){
        return position;
    }

    public String toString(){
        return name;
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.set("name", name);
        json.set("description", description);
        JSONArray arr = new JSONArray();
        for (Parameter p : getParameters()){
            arr.add(p.toJson());
        }
        json.set("parameters", arr);
        return json;
    }
}
