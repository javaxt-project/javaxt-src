package javaxt.utils.src;
import java.util.*;
import static javaxt.utils.Console.console;

//******************************************************************************
//**  Parser Class
//******************************************************************************
/**
 *   Used to parse Java and JavaScript source files and return a list of
 *   classes, methods, and properties within each file. 
 *
 ******************************************************************************/

public class Parser {
    
    private ArrayList<Class> classes;
    private String sourceCode;
    

  //**************************************************************************
  //** main
  //**************************************************************************
    public static void main(String[] arr) throws Exception {
        HashMap<String, String> args = console.parseArgs(arr);
        java.io.File input = new java.io.File(args.get("-input"));
        //java.io.File output = new java.io.File(args.get("-output"));
        
        if (input.isFile()){
            print(new javaxt.io.File(input));
        }
        else{
            
        }
    }
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Parser(javaxt.io.File input) throws Exception {
        parseFile(input);
    }
    
    
  //**************************************************************************
  //** getClasses
  //**************************************************************************
    public ArrayList<Class> getClasses(){
        return classes;
    }
    
    
  //**************************************************************************
  //** getSourceCode
  //**************************************************************************
    public String getSourceCode(){
        return sourceCode;
    }
    
    
  //**************************************************************************
  //** parseFile
  //**************************************************************************
    private void parseFile(javaxt.io.File input){
        String ext = input.getExtension().toLowerCase();
        
        String s = input.getText();
        s = s.replace("\t", "    ").trim();
        s = s.replace("\r\n", "\n").trim();
        s = s.replace("\r", "\n").trim();
        while (s.contains("\n\n")) s = s.replace("\n\n", "\n");
        s = s.trim();
        
        sourceCode = s;
        classes = new ArrayList<>();
        

        ArrayList<Integer> brackets = new ArrayList<>();
        String currNamespace = null;
        Class currClass = null;
        

        StringBuilder code = new StringBuilder();
        int numBrackets = 0;
        
        boolean insideComment = false;
        StringBuilder currComment = new StringBuilder();
        String lastComment = null;
        int lastCommentIndex = 0;
        
        
        for (int i=0; i<s.length(); i++){
            char c = s.charAt(i); 
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);

            
            
            if (c=='/' && n=='/'){
                if (insideComment){
                    currComment.append("//");
                    i = i+2;
                }
                else{
                    String comment = readLine(s, i);
                    i += comment.length()-1;
                    code.append("\n");
                }

            }
            
            else if (c=='/' && n=='*'){
                if (!insideComment){
                    insideComment = true;
                }
                currComment.append("/*");
                i = i+2;
            }
            
            else if (c=='*' && n=='/'){
                currComment.append("*/");
                i = i+2;
                
                if (insideComment){
                    lastComment = currComment.toString();
                    lastCommentIndex = i;
                    //console.log(lastComment);
                    currComment.delete(0, currComment.length());
                    insideComment = false;
                }

            }
            
            else if (c=='"' || c=='\''){
                
                if (insideComment){
                    currComment.append(c);
                }
                else{                    
                    String quote = readQuote(s, i, c);
                    code.append(quote);
                    i = i+quote.length();
                }
            }

            else{
                if (insideComment){
                    currComment.append(c);
                }
                else{
                    
                    if (c==' ' || c=='\n'){
                        
                      //Find start of the word
                        int idx = -1;
                        for (int j=code.length()-1; j>-1; j--){
                            int t = code.charAt(j);
                            if (t==' ' || t=='\n'){
                                idx = j+1;
                                break;
                            }
                        }

                        
                        
                      //Get word
                        String word = null;
                        if (idx>-1){
                            word = code.substring(idx);
                        }
                        else{
                          //Special case for package keywords in java files
                            if (code.toString().equals("package") && ext.equals("java")){
                                word = code.toString();
                            }
                        }
                        
                        
                      //If we have a word...
                        if (word!=null && !word.isBlank()){

                            if (ext.equals("java")){
                            
                                if (word.equals("package")){
                                    String[] arr = readFunction(s, i);
                                    String fn = arr[0].replace(" ", "");
                                    currNamespace = fn;
                                }
                            
                                else if (word.equals("class")){

                                  //Get class name and create new Class
                                    String[] arr = readFunction(s, i);
                                    String fn = arr[0]; //class name followed by extends, implements, etc
                                    arr = fn.split(" "); 
                                    String name = arr[0];
                                    Class cls = new Class(name);
                                    
                                  //Set namespace
                                    cls.setNamespace(currNamespace);
                                    currNamespace = null;

                                    
                                  //Add extensions
                                    for (int x=1; x<arr.length; x++){
                                        String wd = arr[x];
                                        if (wd.equals("extends")){
                                            for (int y=x+1; y<arr.length; y++){
                                                String w = arr[y];
                                                if (w.equals("implements")) break;
                                                cls.addSuper(w);
                                            }
                                        }
                                        if (wd.equals("implements")){
                                            for (int y=x+1; y<arr.length; y++){
                                                String w = arr[y];
                                                if (w.equals("extends")) break;
                                                cls.addInterface(w);
                                            }
                                        }
                                    }
                                    

                                  //Update brackets array (used to find the end of the class)
                                    brackets.add(numBrackets);


                                  //Get class modifiers (public, static, final, etc)
                                    LinkedHashSet<String> modifiers = getModifiers(code.toString(), idx);
                                    if (modifiers.contains("private")) cls.setPublic(false);


                                  //Get last comment and update the class
                                    int lc = getEndOfLastComment(s, i);
                                    if (lc==lastCommentIndex){
                                        String comment = parseComment(lastComment);
                                        cls.setDescription(comment);
                                    }


                                  //Update currClass
                                    if (currClass==null){ 
                                        currClass = cls;
                                    }
                                    else{
                                        cls.setParent(currClass);
                                        currClass = cls;
                                    }


//                                    console.log("-----------------------------");
//                                    console.log(modifiers, "class " + "|" + name + "|");
//                                    console.log("-----------------------------");
//                                    if (cls.isPublic()) console.log(cls.getDescription());
                                }
                                
                                
                                else if (word.equals("public")){
                                    String[] arr = readFunction(s, i);
                                    String fn = arr[0];

                                  //If the public member is not a class...
                                    if (!(" " + fn + " ").contains(" class ")){
                                        
                                        
                                        String comment = null;
                                        int lc = getEndOfLastComment(s, i);
                                        //console.log(lc, lastCommentIndex);
                                        if (lc==lastCommentIndex){
                                            comment = parseComment(lastComment);
                                        }
                                        
                                        
                                        String raw = arr[1];
                                        char lastChar = s.charAt(i+raw.length());
                                        if (lastChar=='{'){
                                            //console.log(fn);
                                            
                                          //Parse method and add to class
                                            currClass.addMember(parseMethod(fn, comment, ext));
                                            
                                          //Move past the function
                                            int end = getEndBacket(s, i);
                                            //console.log(s.substring(i, end));
                                            
                                            if (end>-1) i = end;
                                        }

                                    }
                                }
                                
                                
                                
                            }
                            else if (ext.equals("js")){


                              //Get next few words
                                ArrayList<String> nextWords = new ArrayList<>();
                                int offset = i;
                                for (int x=0; x<3; x++){
                                    String nextWord = getNextWord(s, offset);
                                    if (nextWord==null) break;
                                    offset += nextWord.length();
                                    nextWord = nextWord.trim();
                                    nextWords.add(nextWord);
                                }


                              //Check if the current word is associated with a function
                                boolean isFunction = false;
                                if (!nextWords.isEmpty()){
                                    String nextWord = nextWords.get(0);
                                    if (nextWord.equals("=") || nextWord.equals(":")){
                                        if (nextWords.size()>1){
                                            nextWord = nextWords.get(1);
                                            isFunction = nextWord.equals("function") || nextWord.startsWith("function(");
                                        }
                                    }
                                    else{ 
                                        if (nextWord.startsWith("=function") || nextWord.startsWith(":function")){
                                            nextWord = nextWord.substring(1);
                                            isFunction = nextWord.equals("function") || nextWord.startsWith("function(");
                                        }
                                        else if (nextWord.startsWith("function")){
                                            isFunction = nextWord.equals("function") || nextWord.startsWith("function(");
                                        }
                                    }
                                }
                                
                                
                                
                              //Check if the current word is associated with a json object (e.g. Utils.js)
                                boolean isStruct = false;
                                if (!nextWords.isEmpty()){
                                    String nextWord = nextWords.get(0);
                                    if (nextWord.equals("=")){
                                        if (nextWords.size()>1){
                                            nextWord = nextWords.get(1);
                                            isStruct = nextWord.equals("{") || nextWord.startsWith("{");
                                        }
                                    }
                                    else{ 
                                        if (nextWord.startsWith("={") || nextWord.startsWith("{")){
                                            isStruct = true;
                                        }
                                    }
                                }
                                
                                if (isStruct){
                                    
                                  //Special case for javaxt-style components
                                    if (word.equals("defaultConfig") && currClass!=null){
                                        
                                        
                                        int a = i+s.substring(i).indexOf("{");
                                        int b = getEndBacket(s, a);
                                        String defaultConfig = s.substring(a, b);
                                        
                                        for (Constructor contructor : currClass.getConstructors()){
                                            for (Parameter parameter : contructor.getParameters()){
                                                if (parameter.getName().equals("config")){
                                                    if (parameter.getDescription()==null){
                                                        parameter.setDescription(defaultConfig);
                                                    }
                                                }
                                            }
                                        }
                                        
                                        i+=b;
                                    }
                                    
                                  //Special case for Utils.js
                                    if (word.contains(".")){

                                      //Create class
                                        Class cls = new Class(null);
                                        cls.setNamespace(word);
                                        currClass = cls;
                                        classes.add(cls);


                                        i += s.substring(i).indexOf("{");
                                    }
                                    
                                }
                                
                                

                             //If the current word is a function
                                if (isFunction){
                                    
                                    
                                  //Set function name
                                    String functionName = word.trim();
                                    if (functionName.endsWith(":") || functionName.endsWith("=")){
                                        functionName = functionName.substring(0, functionName.length()-1).trim();
                                    }
                                    //console.log(functionName);
                                    
                                    
                                    
                                  //Get comments associated with the function
                                    String comment = null;
                                    int lc = getEndOfLastComment(s, i);
                                    if (lc==lastCommentIndex){
                                        comment = parseComment(lastComment);
                                    }
                                    //if (comment!=null) console.log(comment);
                                    
                                    
                                  //Parse function
                                    String[] arr = readFunction(s, i);
                                    String fn = arr[0];
                                    String raw = arr[1];
                                    i+=raw.length();
                                    
                                    
                                    
                                    boolean skipBody = true;
                                    boolean addMember = false;
                                    if (functionName.contains(".")){
                                        if (functionName.startsWith("this.")){ //found public method

                                            functionName = functionName.substring(5);
                                            addMember = true;                                            
                                            
                                        }
                                        else{ //found class?
                                            
                                            if (currClass==null){ //this is a bit of a hack, need a better way to test for classes

                                              //Update functionName
                                                currNamespace = functionName.substring(0, functionName.lastIndexOf("."));
                                                functionName = functionName.substring(currNamespace.length()+1);


                                              //Create class
                                                Class cls = new Class(functionName);
                                                currClass = cls;


                                              //Set namespace
                                                cls.setNamespace(currNamespace);
                                                currNamespace = null;

                                                classes.add(cls);


                                                Method m = parseMethod(functionName+fn, null, ext);
                                                Constructor contructor = new Constructor(m.getName());
                                                for (Parameter parameter : m.getParameters()){
                                                    contructor.addParameter(parameter);
                                                }
                                                cls.addMember(contructor);


                                                skipBody = false;
                                            }
                                        }
                                    }
                                    else{ //private member or function is part of json object (e.g. Utils.js)
                                        
                                        if (currClass!=null){
                                            addMember = currClass.getName()==null;
                                        }
                                    }
                                    
                                    
                                    if (addMember){
                                        if (!(fn.startsWith("=") || fn.startsWith(":"))) fn = " = " + fn;
                                        Method m = parseMethod(functionName+fn, comment, ext);
                                        currClass.addMember(m);
                                    }
                                    
                                    
                                  //Move past the function as needed
                                    if (skipBody){
                                        int end = getEndBacket(s, i);
                                        if (end>-1) i = end;
                                    }
                                }
                                
                                

                                
                            }
                        }
                    }
                    
                    if (c=='{'){
                        if (!insideComment) numBrackets++;
                    }
                    if (c=='}'){
                        if (!insideComment) numBrackets--;
                        

                      //Check brackets to identify end of a class. This is only 
                      //used when parsing java code.
                        if (brackets.size()>0)
                        if (numBrackets==brackets.get(brackets.size()-1)){
                            brackets.remove(brackets.size()-1);
                            
                                
                            if (currClass!=null){ 
                                //console.log("----------------------------- //end " + currClass.getName());
                                Class parent = currClass.getParent();
                                if (parent==null) classes.add(currClass);
                                else parent.addMember(currClass);
                                currClass = parent;

                            }
                            
                        }

                    }
                    
                    
                    
                    code.append(c);
                    
                }
            }
        }

    }
    
    
    
  //**************************************************************************
  //** readLine
  //**************************************************************************
  /** Returns a string starting at a given index to the next line break
   */
    private static String readLine(String s, int offset){
        StringBuilder str = new StringBuilder();
        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i);
            str.append(c);
            if (c=='\n') break;
        }
        return str.toString();
    }
    
    
  //**************************************************************************
  //** readQuote
  //**************************************************************************
  /** Returns a string encapsulated by either a single or double quote, 
   *  starting at a given index
   */
    private static String readQuote(String s, int i, char t){ 
        
        StringBuilder str = new StringBuilder();
        str.append(s.charAt(i));
        boolean escaped = false;
        for (int x=i+1; x<s.length(); x++){
            char q = s.charAt(x);
            str.append(q);

            if (q==t){
                if (!escaped){
                    break;
                }
                else{
                    escaped = false;
                }
            }
            else if (q=='\\'){
                escaped = !escaped;
            }
            else{
                escaped = false;
            }
        }
        
        return str.toString();
    }
    
    
  //**************************************************************************
  //** readFunction
  //**************************************************************************
  /** Used to return a class definition, property, or method starting at a 
   *  given offset and ending with either a ";" or "{" character 
   */
    private static String[] readFunction(String s, int offset){
        StringBuilder str = new StringBuilder();
        int idx = offset;
        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i);
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);
            
            if (c=='{' || c==';'){
                idx = i;
                break;
            }
            
            else if (c=='/' && n=='*'){
                i = i+2;
                for (int j=i; j<s.length(); j++){
                    c = s.charAt(j);
                    n = j==s.length()-1 ? ' ' : s.charAt(j+1);
                    if (c=='*' && n=='/'){
                        i = i+2;
                        break;
                    }
                    i++;
                }
            }
            
            else if (c=='/' && n=='/'){
                String comment = readLine(s, i);
                i += comment.length()-1;
                str.append("\n");
            }
            
            else{
                str.append(c);
            }
            
        }
        String raw = s.substring(offset, idx);
        String fn = str.toString().trim();
        fn = fn.replace("\n"," ").trim();
        while (fn.contains("  ")) fn = fn.replace("  ", " ");
        return new String[]{fn, raw};
    }
    
    
  //**************************************************************************
  //** getEndBacket
  //**************************************************************************
  /** Finds the last enclosing bracket in a block of text. Example:
   *  "var fn = function(){ };" returns 22 
   */
    private static int getEndBacket(String s, int offset){

        int numBrackets = 0;        
        boolean insideComment = false;
        
        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i); 
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);
            
            if (c=='/' && n=='/'){
                if (insideComment){
                    i = i+2;
                }
                else{
                    String comment = readLine(s, i);
                    i += comment.length()-1;
                }
            }
            
            else if (c=='/' && n=='*'){
                if (!insideComment){
                    insideComment = true;
                }
                i = i+2;
            }
            
            else if (c=='*' && n=='/'){
                i = i+2;
                if (insideComment){
                    insideComment = false;
                }
            }
            
            else if (c=='"' || c=='\''){
                
                if (!insideComment){
                    String quote = readQuote(s, i, c);
                    i = i+quote.length();
                }
            }
            
            else{
                if (!insideComment){
                    
                    if (c=='{'){
                        numBrackets++;
                    }
                    if (c=='}'){
                        numBrackets--;
                        
                        
                        if (numBrackets==0){
                            return i+1;
                        }
                    }
                }
            }
        }
        return -1;
    }
    
    
    private static String getNextWord(String s, int offset){

        StringBuilder code = new StringBuilder();
        boolean insideComment = false;
        
        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i); 
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);
            
            if (c=='/' && n=='/'){
                if (insideComment){
                    i = i+2;
                }
                else{
                    String comment = readLine(s, i);
                    i += comment.length()-1;
                    code.append("\n");
                }
            }
            
            else if (c=='/' && n=='*'){
                if (!insideComment){
                    insideComment = true;
                }
                i = i+2;
            }
            
            else if (c=='*' && n=='/'){
                i = i+2;
                if (insideComment){
                    insideComment = false;
                }
            }
            
            else{
                if (!insideComment){
                    
                    if (c==' ' || c=='\n'){
                        
                      //Find start of the word
                        int idx = -1;
                        for (int j=code.length()-1; j>-1; j--){
                            int t = code.charAt(j);
                            if (t==' ' || t=='\n'){
                                idx = j+1;
                                break;
                            }
                        }

                        
                        
                      //Get word
                        if (idx>-1){
                            String word = code.substring(idx);
                            if (!word.isBlank()) return word;
                            code = new StringBuilder();
                        }
                    }
                    
                    code.append(c);
                }
            }
        }
        return code.toString();
    }
    
    
    private static LinkedHashSet<String> getModifiers(String s, int offset) {
        //console.log(s);
        int idx = offset;
        for (int i=offset; i>-1; i--){
            char c = s.charAt(i);
            char p = i==0 ? ' ' : s.charAt(i-1);
            
          //If we're passing in code without comments, we don't need the following
            /*
            if (c=='/' && p=='/'){
                idx += readLine(s, offset).length();
                break;
            }
            
            if (c=='/' && p=='*'){
                break;
            }
            */
            
            if (c=='}' || c==';'){
                break;
            }
            
            idx = i;
        }
        String str = s.substring(idx, offset).trim();
        while (str.contains("  ")) str = str.replace("  ", " ");
        LinkedHashSet<String> modifiers = new LinkedHashSet<>();
        for (String word : str.split(" ")){
            word = word.trim();
            if (!word.isEmpty()) modifiers.add(word);
        }
        return modifiers;
    }
    
    private static int getEndOfLastComment(String s, int offset) {

        for (int i=offset; i>-1; i--){
            char c = s.charAt(i);
            char p = i==0 ? ' ' : s.charAt(i-1);


            if (c=='}' || c==';'){
                boolean insideComment = false;
                for (int j=i-1; j>-1; j--){
                    c = s.charAt(j);
                    p = j==0 ? ' ' : s.charAt(j-1);
                    
                    if (c=='\n'){
                        break;
                    }
                    
                    if (c=='/' && p=='/'){
                        insideComment = true;
                    }
                    
                    if (c=='*' && p=='/'){
                        //insideComment = true;
                    }
                }
                
                if (!insideComment){
                    return -1;
                }
            }
            
            if (c=='/' && p=='*'){
                
                boolean insideComment = false;
                for (int j=i-1; j>-1; j--){
                    c = s.charAt(j);
                    p = j==0 ? ' ' : s.charAt(j-1);
                    
                    if (c=='\n'){
                        break;
                    }
                    
                    if (c=='/' && p=='/'){
                        insideComment = true;
                    }
                    
                    if (c=='*' && p=='/'){
                        //insideComment = true;
                    }
                }
                
                if (!insideComment){
                    return i+1;
                }
            }
            

        }
        return -1;
    }
    

    
    private static Method parseMethod(String fn, String comments, String ext){
        
        
      //Parse comments separating description from annotations
        String description = null;
        ArrayList<String> annotations = new ArrayList<>();
        if (comments!=null){
            int idx = 0;
            ArrayList<Integer> indexes = new ArrayList<>();
            for (String row : comments.split("\n")){
                if (row.startsWith("@")) indexes.add(idx);
                idx+=row.length()+1;
            }
            if (indexes.isEmpty()){
                description = comments;
            }
            else{
                int firstComment = indexes.get(0);
                if (firstComment>0){
                    description = comments.substring(0, firstComment);
                }
                for (int i=0; i<indexes.size(); i++){
                    idx = indexes.get(i);
                    if (i==indexes.size()-1){
                        annotations.add(comments.substring(idx).trim());
                    }
                    else{
                        annotations.add(comments.substring(idx, indexes.get(i+1)).trim());
                    }
                }
            }
        }
        
        
        if (ext.equals("java")){
            boolean isStatic = false; //only for java

            String member = fn;
            for (String str : new String[]{"public","private","protected","abstract","static","final"}){
                if (member.startsWith(str + " ")){ 
                    member = member.substring(str.length()+1);
                    if (str.equals("static")) isStatic = true;
                }
            }
            
            
            int a = member.indexOf("(");
            int b = member.indexOf(")");
            if (a>-1 && b>-1){
                String parameters = member.substring(a+1,b).trim();
                member = member.substring(0, a).trim();
                
              //Get method
                Method method;
                int idx = member.lastIndexOf(" ");
                if (idx>-1){
                    String returnType = member.substring(0, idx).trim();
                    String name = member.substring(idx).trim();
                    method = new Method(name);
                    method.setStatic(isStatic);
                    method.setReturnType(returnType);
                }
                else{
                    method = new Constructor(member);
                }
                
              //Set description
                method.setDescription(description);
                
                
              //Add parameters
                if (!parameters.isEmpty()){
                    
                    ArrayList<String> params = new ArrayList<>();
                    StringBuilder str = new StringBuilder();
                    int numBrackets = 0;
                    for (int i=0; i<parameters.length(); i++){
                        char c = parameters.charAt(i);
                        if (c==','){
                            if (numBrackets==0){
                                params.add(str.toString());
                                str = new StringBuilder();
                            }
                            else{
                                str.append(c);
                            }
                        }
                        else{
                            if (c=='<'){
                                numBrackets++;
                            }
                            else if (c=='>'){
                                numBrackets--;
                            }
                            str.append(c);
                        }
                    }
                    params.add(str.toString());
                    

                    
                    for (String param : params){
                        param = param.trim();
                        
                        idx = param.lastIndexOf(" ");
                        if (idx==-1){
                          //Check if we have a spread operator
                            idx = param.indexOf("...");
                            if (idx>-1){
                                String t = param.substring(0, idx);
                                String n = param.substring(idx+3);
                                param = t + "... " + n;
                                idx = param.indexOf(" ");
                            }
                        }
                        
                        String paramName = param.substring(idx).trim();
                        String paramType = param.substring(0, idx).trim();
                        idx = paramType.lastIndexOf(" ");
                        if (idx>-1) paramType = paramType.substring(idx).trim();
                        
                        Parameter parameter = new Parameter(paramName);
                        parameter.setType(paramType);
                        method.addParameter(parameter);
                        
                        for (String annotation : annotations){
                            if (annotation.startsWith("@param " + paramName + " ")){
                                annotation = annotation.substring(("@param " + paramName).length()+1);
                                parameter.setDescription(annotation);
                                break;
                            }
                        }
                        
                    }
                }

                return method;
            }
            else{
                //class or property
            }
            
        }
        else if (ext.equals("js")){
            int a = fn.indexOf("(");
            int b = fn.indexOf(")");
            if (a>-1 && b>-1){

                
              //Create method
                int idx = fn.indexOf("=");
                if (idx==-1) idx = fn.indexOf(":");
                String name = fn.substring(0, idx);
                Method method = new Method(name);

                
              //Set description
                method.setDescription(description);
                
                
              //Add parameters
                String parameters = fn.substring(a+1,b).trim();
                if (!parameters.isEmpty()){
                    for (String param : parameters.split(",")){
                        param = param.trim();

                        
                        Parameter parameter = new Parameter(param);
                        method.addParameter(parameter);
                        
                        for (String annotation : annotations){
                            if (annotation.startsWith("@param " + param + " ")){
                                annotation = annotation.substring(("@param " + param).length()+1);
                                parameter.setDescription(annotation);
                                break;
                            }
                        }
                        
                    }
                }

                return method;
                
                
            }
        }
        
        
//            System.out.println(method);
//            System.out.println("  +" + methodName);
//            System.out.println("  -" + parameters);
//            System.out.println("   returns " + returnType);
        


        
        return null;
    }
    
    private static String parseComment(String comment){
        comment = comment.trim();
        if (comment.startsWith("/*")) comment = comment.substring(2);
        if (comment.endsWith("*/")) comment = comment.substring(0, comment.length()-2);
        boolean insidePre = false;
        StringBuilder str = new StringBuilder();
        for (String row : comment.split("\n")){
            if (!insidePre){
                row = row.trim();
                while (row.startsWith("*")) row = row.substring(1).trim();
                insidePre = (row.contains("<pre>") && !row.contains("</pre>"));
            }
            else{
                if (row.contains("</pre>")) insidePre = false;
            }
            if (str.length()>0){ 
                if (insidePre) str.append("\n");
                else{
                    if (row.startsWith("@")) str.append("\n");
                    else str.append(" ");
                }
            }
            str.append(row);
        }
        return str.toString().trim();
    }
    
    
    
  //**************************************************************************
  //** print
  //**************************************************************************
  /** Used to display all the classes, methods and properties found in a given 
   *  file
   */
    private static void print(javaxt.io.File input) throws Exception {
        ArrayList<Class> classes = new Parser(input).getClasses();

        for (Class cls : classes){
            String className = cls.getName();
            String namespace = cls.getNamespace();
            if (namespace!=null) className = namespace + "." + className;
            System.out.println("-----------------------------------");
            System.out.println("- " + className);
            System.out.println("-----------------------------------");
            String description = cls.getDescription();
            if (description!=null) System.out.println(description);
            
            ArrayList<String> extensions = cls.getSuper();
            if (!extensions.isEmpty()){
                System.out.print("\r\nExtends");
                for (String ext : extensions){
                    System.out.print(" " + ext);
                }
                System.out.println("\r\n");
            }
            

            ArrayList<Constructor> contructors = cls.getConstructors();
            if (!contructors.isEmpty()){
                System.out.println("## Constructors: ");
                for (Constructor m : contructors){
                    printMethod(m);
                }
            }
            
            ArrayList<Method> methods = cls.getMethods();
            if (!methods.isEmpty()){
                System.out.println("\r\n## Methods: ");
                
                for (Method m : methods){
                    printMethod(m);
                }
                
                for (Property p : cls.getProperties()){
                    console.log("  *" + p.getName());
                }
            }
            
            for (Class c : cls.getClasses()){
                if (c.isPublic()){
                    console.log(" +" + c.getName());
                }
            }
            
            
            System.out.println("-----------------------------------");
            System.out.println("\r\n");
            
        }
        
    }
    
    
  //**************************************************************************
  //** printMethod
  //**************************************************************************
    private static void printMethod(Method m){
        String methodName = m.getName();
        String returnType = m.getReturnType();

        ArrayList<Parameter> parameters = m.getParameters();
        String params = "";
        if (!parameters.isEmpty()){
            for (int i=0; i<parameters.size(); i++){
                if (i>0) params += ", ";
                Parameter p = parameters.get(i);
                String t = p.getType();
                if (t!=null) params+= t + " ";
                params += p.getName();
            }
        }


        System.out.println("\r\n+ " + methodName + "(" + params + ")");
        String description = m.getDescription();
        if (description!=null){
            System.out.println("\r\n   - Description:\r\n     " + description);
        }


        if (!parameters.isEmpty()){
            System.out.println("\r\n   - Parameters: ");
            for (Parameter p : parameters){
                String t = p.getType();
                String d = p.getDescription();
                String param = p.getName();
                if (t!=null) param += " (" + p.getType() + ")";
                if (d!=null) param += ": " + d;

                System.out.println("     * " + param);
            }
        }
        
        if (returnType!=null){
            System.out.println("\r\n   - Returns:");
            System.out.println("     " + returnType);
            
        }
    }
}