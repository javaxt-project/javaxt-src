package javaxt.utils.src;
import java.util.*;

public class Config implements Member {

    private String name;
    private String description;
    private String defaultValue;
    private ArrayList<Config> config;
    private Integer position;

    public Config(String name){
        this.name = name;
        this.config = new ArrayList<>();
    }

    public String getName(){
        return name;
    }

    public void setDescription(String description){
        if (description!=null){
            description = description.trim();
            if (description.isEmpty()) description = null;
        }
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    public void setDefaultValue(String defaultValue){
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue(){
        return defaultValue;
    }

    public void addConfig(Config config){
        this.config.add(config);
    }

    public ArrayList<Config> getConfig(){
        return config;
    }

    public void setPosition(Integer position){
        this.position = position;
    }

    public Integer getPosition(){
        return position;
    }
}