package javaxt.utils.src;
import java.util.*;
import javaxt.json.*;


public class Class implements Member, Comparable {
    private String name;
    private String description;
    private Class parent;
    private ArrayList<Member> members;
    private boolean isPublic = true;
    private boolean isInterface = false;
    private String namespace;
    private ArrayList<String> extensions;
    private ArrayList<String> interfaces;


    public Class(String name){
        this.name = name;
        this.members = new ArrayList<>();
        this.extensions = new ArrayList<>();
        this.interfaces = new ArrayList<>();
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

    public boolean isPublic(){
        return isPublic;
    }

    public void setPublic(boolean isPublic){
        this.isPublic = isPublic;
    }

    public boolean isInterface(){
        return isInterface;
    }

    public void setInterface(boolean isInterface){
        this.isInterface = isInterface;
    }

    public void setNamespace(String namespace){
        this.namespace = namespace;
    }

    public String getNamespace(){
        return namespace;
    }

    public void addSuper(String className){
        extensions.add(className);
    }

    public ArrayList<String> getSuper(){
        return extensions;
    }

    public void addInterface(String className){
        interfaces.add(className);
    }

    public ArrayList<String> getInterfaces(){
        return interfaces;
    }

    public void addMember(Member member){
        members.add(member);
    }

    public ArrayList<Member> getMembers(){
        return members;
    }

    public ArrayList<Class> getClasses(){
        ArrayList<Class> classes = new ArrayList<>();
        for (Member member : members){
            if (member instanceof Class) classes.add((Class) member);
        }
        return classes;
    }

    public ArrayList<Constructor> getConstructors(){
        ArrayList<Constructor> constructors = new ArrayList<>();
        for (Member member : members){
            if (member instanceof Method){
                Method m = (Method) member;
                if (m instanceof Constructor){
                    constructors.add((Constructor) m);
                }
            }
        }
        return constructors;
    }

    public ArrayList<Method> getMethods(){
        ArrayList<Method> methods = new ArrayList<>();
        for (Member member : members){
            if (member instanceof Method){
                Method m = (Method) member;
                if (m instanceof Constructor){
                    //System.out.println("-->" + m.getName());
                }
                else{
                    methods.add(m);
                }
            }
        }
        return methods;
    }

    public ArrayList<Property> getProperties(){
        ArrayList<Property> properties = new ArrayList<>();
        for (Member member : members){
            if (member instanceof Property) properties.add((Property) member);
        }
        return properties;
    }

    public void setParent(Class parent){
        this.parent = parent;
    }

    public Class getParent(){
        return parent;
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        json.set("name", name);
        json.set("description", description);
        JSONArray methods = new JSONArray();
        for (Method m : getMethods()){
            methods.add(m.toJson());
        }
        json.set("methods", methods);
        JSONArray properties = new JSONArray();
        for (Property p : getProperties()){
            properties.add(p.toJson());
        }
        json.set("properties", properties);
        JSONArray classes = new JSONArray();
        for (Class c : getClasses()){
            classes.add(c.toJson());
        }
        json.set("classes", classes);
        return json;
    }

    public String toString(){
        if (namespace==null || namespace.isBlank()) return name;
        return namespace + "." + name;
    }

    public int hashCode(){
        return toString().hashCode();
    }

    public boolean equals(Object obj){
        if (obj!=null){
            if (obj instanceof Class){
                return toString().equals(obj.toString());
            }
        }
        return false;
    }

    public int compareTo(Object obj){
        if (obj instanceof Class){
            return toString().compareTo(obj.toString());
        }
        return -1;
    }
}