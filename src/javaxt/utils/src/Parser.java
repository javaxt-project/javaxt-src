package javaxt.utils.src;
import java.util.*;
import javaxt.json.JSONObject;
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

      //Get input file or directory
        String path = args.get("-input");
        if (path==null){
            if (arr.length>0){
                path = arr[0];
            }
            else{
                System.out.println("-input file or directory is required");
                return;
            }
        }



      //Get test file as needed
        String expectedOutput = null;
        String test = args.get("-test");
        if (test!=null) expectedOutput = new javaxt.io.File(test).getText();



      //Generate output
        java.io.File input = new java.io.File(path);
        if (input.isFile()){
            String output = parse(new javaxt.io.File(input));
            if (expectedOutput==null){
                System.out.print(output);
            }
            else{
                if (output.equals(expectedOutput)){
                    System.out.println(input + " [PASS]");
                }
                else{
                    System.out.println(input + " [FAILED] <------");
                }
            }
        }
        else{
            String[] filter = new String[]{"*.js", "*.java"};
            for (javaxt.io.File file : new javaxt.io.Directory(input).getFiles(filter, true)){
                String output = parse(file);
                if (expectedOutput==null){
                    System.out.print(output);
                }
                else{
                    String t = expectedOutput.substring(0, output.length());
                    if (t.equals(output)){
                        System.out.println(file + " [PASS]");
                        expectedOutput = expectedOutput.substring(output.length());
                    }
                    else{
                        System.out.println(file + " [FAILED] <------");
                        for (int i=0; i<output.length(); i++){
                            char a = output.charAt(i);
                            char b = expectedOutput.charAt(i);
                            if (a!=b){
                                int start = i-50;
                                if (start<0) start = 0;
                                int end = Math.min(i + 50, output.length());
                                //, expectedOutput.length()


                                System.out.println(output.substring(start, i) + "|" + output.substring(i, end));
                                System.out.println("--------------------------------------------");
                                System.out.println(expectedOutput.substring(start, i) + "|" + expectedOutput.substring(i, end));

                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Parser(javaxt.io.File file) throws Exception {
        this(file.getText(), file.getExtension());
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Parser(String text, String type) throws Exception {
        parse(text, type);
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
  //** parse
  //**************************************************************************
    private void parse(String s, String ext){
        sourceCode = s;

        if (ext==null) ext = "";
        else ext = ext.trim().toLowerCase();


        if (ext.equals("js") || ext.equals("javascript")){
            classes = getClasses(s);
        }
        else if (ext.equals("java")) {
            classes = new ArrayList<>();
            String packageName = null;


            int i=0;
            Word word, p1 = null, p2 = null;
            while ((word = getNextWord(s, i))!=null){

                String str = word.toString();
                if (str.endsWith("package") && packageName==null){
                    Word nextWord = getNextWord(s, word.end);
                    packageName = nextWord.toString();

                    int idx = packageName.indexOf(";");
                    if (idx>-1){
                        packageName = packageName.substring(0, idx);
                        i = (nextWord.end-idx)+1;
                    }
                    else{
                        i = nextWord.end+1;
                    }

                }
                else if (str.endsWith("class") || str.endsWith("interface")){

                    int start = getStartBacket(s, i, '{');
                    int end = getEndBacket(s, start, '{');

                    Word nextWord = getNextWord(s, word.end);
                    String name = nextWord.toString();

                    Class cls = new Class(name);
                    cls.setNamespace(packageName);
                    addModifiers(cls, word, p1, p2);
                    addExtensions(cls, s.substring(word.end, start));
                    boolean isInterface = str.contains("interface");
                    cls.setInterface(isInterface);
                    addMembers(cls, s.substring(start, end));
                    cls.setPosition(i);
                    classes.add(cls);


                    i = end+1;
                }
                else{
                    i = word.end+1;
                }

                p2 = p1;
                p1 = word;
            }
        }
        else{
            throw new IllegalArgumentException("Unsupported file type: " + ext);
        }
    }


  //**************************************************************************
  //** addMembers
  //**************************************************************************
  /** Used to find Java methods and properties and add them to the given class
   */
    private static void addMembers(Class cls, String s){


        s = s.trim();
        if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length()-1).trim();



        int i=0;
        Word word;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString();
            if (str.equals("public") || str.equals("private") || str.equals("protected")
                || str.equals("class") || str.equals("interface")
            ){

                boolean isPublic = !(str.equals("private") || str.equals("protected"));

                Comment comment = parseComment(word.lastComment);


                int a = getStartBacket(s, i, '(');
                int b = getStartBacket(s, i, ';');
                int c = getStartBacket(s, i, '{');
                int d = getStartBacket(s, i, '=');
                if (a==-1) a = Integer.MAX_VALUE;
                if (b==-1) b = Integer.MAX_VALUE;
                if (c==-1) c = Integer.MAX_VALUE;
                if (d==-1) d = Integer.MAX_VALUE;



                if (c<a && c<b && c<d){ //found class or interface

                    int start = c;
                    int end = getEndBacket(s, start, '{');
                    String fn = s.substring(i, start);


                    ArrayList<Word> words = new ArrayList<>();
                    int j=0, idx=-1;
                    Word w;
                    while ((w = getNextWord(fn, j))!=null){
                        String wd = w.toString();
                        if (!wd.isEmpty()){
                            words.add(w);
                            if (wd.equals("class") || wd.equals("interface")){
                                idx = words.size()-1;
                            }
                        }
                        j = w.end+1;
                    }


                    if (idx>-1){
                        word = words.get(idx);
                        Word nextWord = words.get(idx+1);
                        String name = nextWord.toString();
                        Word p1 = idx>0 ? words.get(idx-1) : null;
                        Word p2 = idx>1 ? words.get(idx-2) : null;

                        String extensions = "";
                        for (j=idx+1; j<words.size(); j++){
                            if (!extensions.isEmpty()) extensions += " ";
                            extensions += words.get(j).toString();
                        }


                        Class innerClass = new Class(name);
                        addModifiers(innerClass, word, p1, p2);
                        addExtensions(innerClass, extensions);
                        boolean isInterface = word.toString().equals("interface");
                        innerClass.setInterface(isInterface);
                        addMembers(innerClass, s.substring(start, end));
                        innerClass.setPosition(nextWord.end);
                        cls.addMember(innerClass);
                    }


                    i = end+1;
                }
                else{ //found method or property

                    if (a<b && a<d){ //found method


                      //Get parameters
                        int start = a;
                        int end = getEndBacket(s, start, '(');
                        String params = s.substring(start, end);


                      //Get keywords before the parameters
                        String fn = s.substring(word.end, a);
                        ArrayList<String> arr = new ArrayList<>();
                        int j=0;
                        Word w;
                        while ((w = getNextWord(fn, j))!=null){
                            arr.add(w.toString().trim());
                            j = w.end+1;
                        }


                      //Get method name
                        String name = arr.get(arr.size()-1);


                      //Get return type
                        String returnType = null;
                        boolean isStatic = false;
                        if (arr.size()>1){
                            for (int k=0; k<arr.size()-1; k++){
                                String t = arr.get(k);
                                if (t.equals("final") || t.equals("static") || t.equals("abstract")){
                                    if (t.equals("static")){
                                        isStatic = true;
                                    }
                                }
                                else{
                                    if (returnType==null) returnType = t;
                                    else returnType += " " + t;
                                }
                            }
                            returnType = returnType.trim();
                            if (returnType.isEmpty()) returnType = null;
                        }


                      //Create method
                        Method method;
                        if (returnType==null){
                            method = new Constructor(name);
                        }
                        else{
                            method = new Method(name);
                            method.setReturnType(returnType);
                        }
                        method.setPublic(isPublic);
                        method.setDescription(comment.getDescription());
                        method.setPosition(word.end);
                        cls.addMember(method);


                      //Add modifiers
                        method.setStatic(isStatic);
                        ArrayList<String> annotations = comment.getAnnotations();
                        for (String annotation : annotations){
                            if (annotation.startsWith("@deprecated")){
                                String additionalInfo = annotation.substring("@deprecated".length()).trim();
                                method.setDeprecated(true, additionalInfo);
                            }
                        }


                      //Add parameters
                        for (Parameter parameter : getParameters(params, annotations)){
                            method.addParameter(parameter);
                        }


                      //Find end of function
                        a = getStartBacket(s, end, '{');
                        b = getStartBacket(s, end, ';');
                        if (a==-1) a = Integer.MAX_VALUE;
                        if (b==-1) b = Integer.MAX_VALUE;
                        if (a<b){
                            end = getEndBacket(s, a, '{');
                        }
                        else if (b<a){
                            end = b;
                        }

                        i = end+1;

                    }
                    else if (b<a || d<a){ //found property or class


                        if (b<c){ //found property
                            String p = s.substring(word.end, b);


                            StringBuilder prop = new StringBuilder();
                            int j=0;
                            Word w;
                            while ((w = getNextWord(p, j))!=null){
                                String wd = w.toString();
                                if (!wd.isEmpty()){
                                    if (prop.length()>0) prop.append(" ");
                                    prop.append(wd);
                                }
                                j = w.end+1;
                            }
                            p = prop.toString().trim();

                            String defaultValue = null;
                            int idx = getStartBacket(p, 0, '=');
                            if (idx>-1){
                                defaultValue = p.substring(idx+1).trim();
                                p = p.substring(0, idx).trim();
                            }



                            ArrayList<String> arr = new ArrayList<>();
                            j=0;
                            while ((w = getNextWord(p, j))!=null){
                                arr.add(w.toString().trim());
                                j = w.end+1;
                            }

                          //Get property name
                            String name = arr.get(arr.size()-1);


                          //Get property type
                            String type = null;
                            boolean isStatic = false;
                            if (arr.size()>1){
                                for (int k=0; k<arr.size()-1; k++){
                                    String t = arr.get(k);
                                    if (t.equals("final") || t.equals("static") || t.equals("abstract")){
                                        if (t.equals("static")){
                                            isStatic = true;
                                        }
                                    }
                                    else{
                                        if (type==null) type = t;
                                        else type += " " + t;
                                    }
                                }
                                type = type.trim();
                                if (type.isEmpty()) type = null;
                            }

                            //console.log(cls.toString() + " - " + name + " (" + type + ") " +"|"+defaultValue);

                            Property property = new Property(name);
                            property.setPublic(isPublic);
                            property.setDescription(comment.getDescription());
                            property.setType(type);
                            property.setStatic(isStatic);
                            property.setDefaultValue(defaultValue);
                            property.setPosition(word.end);
                            cls.addMember(property);


                            i = b+1;
                        }
                        else{

                            i = word.end+1;

                        }
                    }
                    else{

                        i = word.end+1;
                    }
                }

            }
            else{

                i = word.end+1;

            }
        }
    }


  //**************************************************************************
  //** addModifiers
  //**************************************************************************
    private static void addModifiers(Class cls, Word word, Word p1, Word p2){
        String lastComment = word.lastComment;
        if (lastComment==null){
            if (p1!=null){
                String type = p1.toString();
                int idx = type.lastIndexOf(";");
                if (idx>-1) type = type.substring(idx+1);
                idx = type.lastIndexOf("}");
                if (idx>-1) type = type.substring(idx+1);

                if (type.equals("private") || type.equals("protected")){
                    cls.setPublic(false);
                    p2 = null;
                }
                else if (type.equals("public")){
                    p2 = null;
                }

                lastComment = p1.lastComment;

                if (lastComment==null){
                    if (p2!=null){
                        type = p2.toString();
                        idx = type.lastIndexOf(";");
                        if (idx>-1) type = type.substring(idx+1);
                        idx = type.lastIndexOf("}");
                        if (idx>-1) type = type.substring(idx+1);
                        if (type.equals("private") || type.equals("protected")){
                            cls.setPublic(false);
                        }

                        lastComment = p2.lastComment;
                    }
                }
            }
        }
        if (lastComment!=null){
            cls.setDescription(parseComment(lastComment).getDescription());
        }
    }


  //**************************************************************************
  //** addExtensions
  //**************************************************************************
  /** Used to search for "extends" and "implements" keywords after a Java
   *  class name
   */
    private static void addExtensions(Class cls, String fn){
        if (fn==null) return;
        fn = fn.trim();
        if (fn.isEmpty()) return;


        ArrayList<String> arr = new ArrayList<>();
        int j=0;
        Word word;
        while ((word = getNextWord(fn, j))!=null){
            arr.add(word.toString());
            j = word.end+1;
        }



      //Add extensions
        for (int x=1; x<arr.size(); x++){
            String wd = arr.get(x);
            if (wd.equals("extends")){
                for (int y=x+1; y<arr.size(); y++){
                    String w = arr.get(y);
                    if (w.equals("implements")) break;
                    cls.addSuper(w);
                }
            }
            if (wd.equals("implements")){
                for (int y=x+1; y<arr.size(); y++){
                    String w = arr.get(y);
                    if (w.equals("extends")) break;
                    cls.addInterface(w);
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
  //** getStartBacket
  //**************************************************************************
    private static int getStartBacket(String s, int offset, char startBracket){

        boolean insideComment = false;

        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i);
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);

            if (c=='/' && n=='/'){
                if (insideComment){
                    i = i+1;
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
                i = i+1;
            }

            else if (c=='*' && n=='/'){
                i = i+1;
                if (insideComment){
                    insideComment = false;
                }
            }

            else if (c=='"' || c=='\''){

                if (!insideComment){
                    String quote = readQuote(s, i, c);
                    i = i+quote.length()-1;
                }
            }

            else{
                if (!insideComment){

                    if (c==startBracket) return i;
                }
            }
        }
        return -1;
    }


  //**************************************************************************
  //** getEndBacket
  //**************************************************************************
  /** Returns the position of the last enclosing bracket in a block of text.
   *  Example: "var fn = function(){ };" returns 22
   *  @param startBracket Expects either a '{' or '[' character
   */
    private static int getEndBacket(String s, int offset, char startBracket){

        char endBracket;
        if (startBracket=='{') endBracket = '}';
        else if (startBracket=='(') endBracket = ')';
        else if (startBracket=='[') endBracket = ']';
        else return -1;


        int numBrackets = 0;
        boolean insideComment = false;

        for (int i=offset; i<s.length(); i++){
            char c = s.charAt(i);
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);

            if (c=='/' && n=='/'){
                if (insideComment){
                    i = i+1;
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
                i = i+1;
            }

            else if (c=='*' && n=='/'){
                i = i+1;
                if (insideComment){
                    insideComment = false;
                }
            }

            else if (c=='"' || c=='\''){

                if (!insideComment){
                    String quote = readQuote(s, i, c);
                    i = i+quote.length()-1;
                }
            }

            else{
                if (!insideComment){

                    if (c==startBracket){
                        numBrackets++;
                    }
                    if (c==endBracket){
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


  //**************************************************************************
  //** getNextWord
  //**************************************************************************
  /** Returns the next word in the given string that is not inside a comment
   *  block
   */
    private static Word getNextWord(String s, int offset){

        if (offset+1>s.length()) return null;


        StringBuilder code = new StringBuilder();
        boolean insideComment = false;
        StringBuilder currComment = new StringBuilder();
        String lastComment = null;
        int lastCommentIndex = 0;


        int i=offset;
        for (; i<s.length(); i++){
            char c = s.charAt(i);
            char n = i==s.length()-1 ? ' ' : s.charAt(i+1);


            if (c=='/' && n=='/'){
                if (insideComment){
                    currComment.append("//");
                    i = i+1;
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
                i = i+1;
            }

            else if (c=='*' && n=='/'){
                currComment.append("*/");
                i = i+1;

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
                    i = i+quote.length()-1;
                }
            }

            else{
                if (insideComment){
                    currComment.append(c);
                }
                else{

                    if ((c==' ' || c=='\r' || c=='\n' || c=='\t') && code.length()>0){

                      //Find start of the word
                        int idx = -1;
                        boolean foundEnd = false;
                        for (int j=code.length()-1; j>-1; j--){
                            int t = code.charAt(j);
                            if (t==' ' || c=='\r' || c=='\n' || c=='\t'){
                                if (foundEnd){
                                    idx = j+1;
                                    break;
                                }
                            }
                            else{
                                foundEnd = true;
                            }
                        }



                      //Get word
                        String word;
                        if (idx>-1){
                            word = code.substring(idx);
                        }
                        else{
                            word = code.toString();
                        }


                        if (!word.isBlank()){
                            return new Word(word.trim(), i, lastComment, lastCommentIndex);
                        }
                        else{
                            code = new StringBuilder();
                        }
                    }

                    code.append(c);
                }
            }
        }

        if (insideComment) return null;
        String word = code.toString().trim();
        if (word.isBlank()) return null;
        return new Word(word, i, lastComment, lastCommentIndex);
    }


  //**************************************************************************
  //** parseComment
  //**************************************************************************
    private static Comment parseComment(String comment){

        if (comment==null) comment = "";
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
        String comments = str.toString().trim();


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

        return new Comment(description, annotations);
    }


  //**************************************************************************
  //** getClasses
  //**************************************************************************
  /** Used to extract classes from a given block of JavaScript
   */
    private static ArrayList<Class> getClasses(String s){
        ArrayList<Class> classes = new ArrayList<>();
        ArrayList<JSONObject> orphanedFunctions = new ArrayList<>();

        int i=0;
        Word word, p1 = null, p2 = null;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString();
            if (str.contains("function")){

              //Get function name
                JSONObject fn = getFunctionName(word, p1, p2);


              //Get parameters
                int start = getStartBacket(s, i, '(');
                int end = getEndBacket(s, start, '(');
                String params = s.substring(start, end);


              //Read function
                start = getStartBacket(s, i, '{');
                end = getEndBacket(s, start, '{');
                str = s.substring(start, end);


                if (fn!=null){
                    String functionName = fn.get("name").toString();
                    if (functionName.contains(".")){ //javaxt-style class

                      //Update functionName
                        String namespace = functionName.substring(0, functionName.lastIndexOf("."));
                        functionName = functionName.substring(namespace.length()+1);


                      //Create class
                        Class cls = new Class(functionName);
                        cls.setNamespace(namespace);
                        Comment comment = parseComment(fn.get("comment").toString());
                        cls.setDescription(comment.getDescription());
                        cls.setPosition(i);
                        classes.add(cls);


                      //Add constructor
                        Constructor contructor = new Constructor(functionName);
                        contructor.setPosition(start);
                        for (Parameter parameter : getParameters(params, comment.getAnnotations())){
                            contructor.addParameter(parameter);
                        }
                        cls.addMember(contructor);


                      //Get config
                        for (Config config : getDefaultConfig(str)){
                            cls.addConfig(config);
                        }


                      //Get functions
                        for (Method function : getFunctions(str)){
                            if (function.isPublic()){
                                cls.addMember(function);
                            }
                        }

                    }
                    else{

                        //TODO: add static functions to anonymous class (e.g. Utils.js)

                    }
                }

                i = end+1;
            }
            else{
                i = word.end+1;
            }
            p2 = p1;
            p1 = word;
        }


        return classes;
    }


  //**************************************************************************
  //** getFunctions
  //**************************************************************************
  /** Used to find JavaScript functions in a given string
   */
    private static ArrayList<Method> getFunctions(String s){
        ArrayList<Method> functions = new ArrayList<>();

        s = s.trim();
        if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length()-1).trim();

        int i=0;
        Word word, p1 = null, p2 = null;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString();
            if (str.contains("function")){
                JSONObject fn = getFunctionName(word, p1, p2);
                String functionName = fn.get("name").toString();
                Comment comment = parseComment(fn.get("comment").toString());
                Method function = new Method(functionName);
                function.setDescription(comment.getDescription());
                function.setPublic(fn.get("isPublic").toBoolean());
                function.setPosition(word.end);
                functions.add(function);


              //Get parameters
                int start = getStartBacket(s, i, '(');
                int end = getEndBacket(s, start, '(');
                String params = s.substring(start, end);
                for (Parameter parameter : getParameters(params, comment.getAnnotations())){
                    function.addParameter(parameter);
                }


              //Read function
                start = getStartBacket(s, i, '{');
                end = getEndBacket(s, start, '{');
                //str = s.substring(start, end);

                i = end+1;

            }
            else{

                if (str.contains("{")){ //skip properties
                    int start = getStartBacket(s, i, '{');
                    int end = getEndBacket(s, start, '{');
                    //String x = s.substring(start, end);
                    //console.log(x);

                    i = end+1;
                }
                else{
                    i = word.end+1;
                }
            }
            p2 = p1;
            p1 = word;
        }

        return functions;
    }


  //**************************************************************************
  //** getFunctionName
  //**************************************************************************
  /** Returns the name of a JavaScript function found in the given word or
   *  previous words, along with any comments. Assumes function name is
   *  defined as a variable (e.g. "fn = function(a, b){};") or as property
   *  (e.g. "fn: function(a, b){}").
   */
    private static JSONObject getFunctionName(Word word, Word p1, Word p2){

        String functionName = null;
        String lastComment = null;
        boolean hasColon = false;


        String str = word.toString();
        int idx = str.indexOf("function");
        str = str.substring(0, idx);


        if (str.contains("=") || str.contains(":")){

            int a = str.indexOf("=");
            int b = str.indexOf(":");

            if (a>b){
                str = str.substring(0, a).trim();
            }
            else{
                str = str.substring(0, b).trim();
            }

            if (str.isEmpty()){
                if (p1==null) return null;
                functionName = p1.toString().trim();
                lastComment = p1.lastComment;
                hasColon = b>-1;

            }
            else{
                functionName = str;
                lastComment = word.lastComment;
                hasColon = b>-1;
            }
        }
        else{
            if (p1==null) return null;
            str = p1.toString().trim();

            if (str.equals("=") || str.equals(":")){
                if (p2==null) return null;
                functionName = p2.toString().trim();
                lastComment = p2.lastComment;
                hasColon = str.equals(":");
            }
            else{
                int a = str.indexOf("=");
                int b = str.indexOf(":");

                if (a>b){
                    str = str.substring(0, a).trim();
                }
                else{
                    str = str.substring(0, b).trim();
                }
                functionName = str;
                lastComment = p1.lastComment;
                hasColon = b>-1;
            }
        }

        if (functionName==null) return null;
        boolean isPublic = false;
        if (functionName.startsWith("this.")){
            functionName = functionName.substring(5);
            isPublic = true;
        }
        else{
            if (hasColon) isPublic = true;
        }

        JSONObject json = new JSONObject();
        json.set("name", functionName);
        json.set("comment", lastComment);
        json.set("isPublic", isPublic);
        return json;
    }


  //**************************************************************************
  //** getParameters
  //**************************************************************************
    private static ArrayList<Parameter> getParameters(String s, ArrayList<String> annotations){
        ArrayList<Parameter> parameters = new ArrayList<>();

        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length()-1).trim();



      //Get words (skips comments)
        StringBuilder words = new StringBuilder();
        int i=0;
        Word word;
        while ((word = getNextWord(s, i))!=null){
            String w = word.toString();
            if (!w.isEmpty()){
                if (i>0) words.append(" ");
                words.append(word.toString());
            }
            i = word.end+1;
        }

        if (words.length()==0) return parameters;



      //Get parameters
        ArrayList<String> params = new ArrayList<>();
        i=0;
        int numBrackets = 0;
        StringBuilder currWord = new StringBuilder();
        for (; i<words.length(); i++){
            boolean append = true;
            char c = words.charAt(i);
            if (c=='<') numBrackets++;
            else if (c=='>') numBrackets--;
            else{
                if (c==',' && numBrackets==0){
                    params.add(currWord.toString().trim());
                    currWord = new StringBuilder();
                    append = false;
                }
            }
            if (append) currWord.append(c);
        }
        params.add(currWord.toString().trim());


      //Parse parameters
        for (String str : params){
            if (str.isEmpty()) continue;


          //Check if we have a spread operator (Java only)
            int idx = str.lastIndexOf(" ");
            if (idx==-1){
                idx = str.indexOf("...");
                if (idx>-1){
                    String t = str.substring(0, idx);
                    String n = str.substring(idx+3);
                    str = t + "... " + n;
                    idx = str.lastIndexOf(" ");
                }
            }



            String name = str.substring(idx+1);
            if (name.isEmpty()) continue;

            Parameter parameter = new Parameter(name);
            if (idx>-1) parameter.setType(str.substring(0, idx).trim());


            if (annotations!=null){
                for (String annotation : annotations){
                    if (annotation.startsWith("@param " + name + " ")){
                        annotation = annotation.substring(("@param " + name).length()+1);
                        parameter.setDescription(annotation);
                        break;
                    }
                }
            }

            parameters.add(parameter);
        }
        return parameters;
    }


  //**************************************************************************
  //** getDefaultConfig
  //**************************************************************************
    private static ArrayList<Config> getDefaultConfig(String s){

        int i=0;
        Word word;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString();
            if (str.contains("defaultConfig")){

              //Parse config
                int start = getStartBacket(s, i, '{');
                int end = getEndBacket(s, start, '{');
                return parseConfig(s.substring(start, end));

                //i = end+1;
            }
            else{
                i = word.end+1;
            }
        }

        return new ArrayList<>();
    }


  //**************************************************************************
  //** parseConfig
  //**************************************************************************
  /** Used to parse javascript containing config settings
   */
    private static ArrayList<Config> parseConfig(String defaultConfig){
        ArrayList<Config> arr = new ArrayList<>();


        String s = defaultConfig.trim();
        if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length()-1).trim();
        else if (s.startsWith("[") && s.endsWith("]")) s = s.substring(1, s.length()-1).trim();
        else return arr;



        int i=0;
        Word word, prevWord = null;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString().trim();
            if (str.equals(":")){

              //Get next word
                Word nextWord = word.getNextWord(s);
                if (nextWord==null) break;


              //Create config using the previous word
                String key = prevWord.toString().trim();
                Config config = new Config(key);
                config.setDescription(parseComment(prevWord.lastComment).getDescription());
                config.setPosition(prevWord.end);
                arr.add(config);

              //Get config value and update i
                i = getDefaultValue(word.end, nextWord, config, s);

            }
            else if (str.endsWith(":")){

              //Get next word
                Word nextWord = word.getNextWord(s);
                if (nextWord==null) break;


              //Create config using the current word
                String key = str.substring(0, str.length()-1);
                Config config = new Config(key);
                config.setDescription(parseComment(word.lastComment).getDescription());
                config.setPosition(word.end);
                arr.add(config);


              //Get config value and update i
                i = getDefaultValue(word.end, nextWord, config, s);

            }
            else if (str.startsWith(":")){

              //Create config using the previous word
                String key = prevWord.toString().trim();
                Config config = new Config(key);
                config.setDescription(parseComment(prevWord.lastComment).getDescription());
                config.setPosition(prevWord.end);
                arr.add(config);




              //Get config value and update i
                i = getDefaultValue(i, word, config, s);

            }

            else if (str.contains(":")){

                str = word.toString();
                int idx = str.indexOf(":");
                int len = str.length();
                i = word.end-(len-idx);
                word = new Word(str.substring(0, idx), i, word.lastComment, word.lastCommentIndex);


              //Get next word
                Word nextWord = word.getNextWord(s);
                if (nextWord==null) break;


                String key = word.toString().trim();


                Config config = new Config(key);
                config.setDescription(parseComment(word.lastComment).getDescription());
                config.setPosition(word.end);
                arr.add(config);

              //Get config value and update i
                i = getDefaultValue(word.end, nextWord, config, s);
            }

            else{

                i = word.end+1;
            }

            prevWord = word;
        }

        return arr;
    }


  //**************************************************************************
  //** getDefaultValue
  //**************************************************************************
    private static int getDefaultValue(int offset, Word nextWord, Config config, String s){

        String str = nextWord.toString().trim();
        if (str.startsWith("{") || str.startsWith("[")){

            char startBracket = str.startsWith("{") ? '{' : '[';
            int start = getStartBacket(s, offset, startBracket);
            int end = getEndBacket(s, start, startBracket);
            str = s.substring(start, end);
            config.setDefaultValue(str);

            for (Config c : parseConfig(str)){
                config.addConfig(c);
            }

            return end+1;
        }
        else{
            String val = str.trim();
            if (val.startsWith(":")) val = val.substring(1);
            if (val.endsWith(",")) val = val.substring(0, val.length()-1);
            config.setDefaultValue(val.trim());

            return nextWord.end+1;
        }

    }


  //**************************************************************************
  //** print
  //**************************************************************************
  /** Used to display all the classes, methods and properties found in a given
   *  file
   */
    private static String parse(javaxt.io.File input) throws Exception {

        Printer printer = new Printer();
        ArrayList<Class> classes = new Parser(input).getClasses();
        for (Class cls : classes){
            String className = cls.getName();
            String namespace = cls.getNamespace();
            boolean isInterface = cls.isInterface();
            if (namespace!=null) className = namespace + "." + className;
            printer.println("-----------------------------------");
            printer.println("- " + className + (isInterface? " (Interface)" : ""));
            printer.println("-----------------------------------");
            String description = cls.getDescription();
            if (description!=null) printer.println(description);

            ArrayList<String> extensions = cls.getSuper();
            if (!extensions.isEmpty()){
                printer.print("\r\nExtends");
                for (String ext : extensions){
                    printer.print(" " + ext);
                }
                printer.println("\r\n");
            }


            ArrayList<Constructor> contructors = cls.getConstructors();
            if (!contructors.isEmpty()){
                printer.println("\r\n## Constructors: ");
                for (Constructor c : contructors){
                    printMethod(c, printer);
                }
            }


            ArrayList<Config> config = cls.getConfig();
            if (!config.isEmpty()){
                printer.println("\r\n## Config: ");
                for (Config c : config){
                    printConfig(c, printer);
                }
            }


            ArrayList<Property> properties = cls.getProperties();
            if (!properties.isEmpty()){
                printer.println("\r\n## Properties: ");
                for (Property p : properties){
                    if (p.isPublic()){
                        printProperty(p, printer);
                    }
                }
            }


            ArrayList<Method> methods = cls.getMethods();
            if (!methods.isEmpty()){
                printer.println("\r\n## Methods: ");
                for (Method m : methods){
                    if (m.isPublic()){
                        printMethod(m, printer);
                    }
                }
            }


            for (Class c : cls.getClasses()){
                if (c.isPublic()){
                    printer.println(" +" + c.getName());
                }
            }


            printer.println("-----------------------------------");
            printer.println("\r\n");

        }

        return printer.toString();
    }


  //**************************************************************************
  //** printMethod
  //**************************************************************************
    private static void printMethod(Method m, Printer printer){
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


        printer.println("\r\n+ " + methodName + "(" + params + ")");
        String description = m.getDescription();
        if (description!=null){
            printer.println("\r\n   - Description:\r\n     " + description);
        }


        if (!parameters.isEmpty()){
            printer.println("\r\n   - Parameters: ");
            for (Parameter p : parameters){
                String t = p.getType();
                String d = p.getDescription();
                String param = p.getName();
                if (t!=null) param += " (" + p.getType() + ")";
                if (d!=null) param += ": " + d;

                printer.println("     * " + param);
            }
        }

        if (returnType!=null){
            printer.println("\r\n   - Returns:");
            printer.println("     " + returnType);

        }
    }


  //**************************************************************************
  //** printProperty
  //**************************************************************************
    private static void printProperty(Property property, Printer printer){
        String name = property.getName();
        String description = property.getDescription();
        String defaultValue = property.getDefaultValue();


        printer.println("\r\n+ " + name);
        if (description!=null){
            printer.println("\r\n   - Description:\r\n     " + description);
        }


        printer.println("\r\n   - Default:\r\n     " + defaultValue);
    }


  //**************************************************************************
  //** printConfig
  //**************************************************************************
    private static void printConfig(Config config, Printer printer){
        String name = config.getName();
        String description = config.getDescription();
        String defaultValue = config.getDefaultValue();
        ArrayList<Config> arr = config.getConfig();

        if (!arr.isEmpty()) defaultValue = null;



        printer.println("\r\n+ " + name);
        if (description!=null){
            printer.println("\r\n   - Description:\r\n     " + description);
        }

        if (defaultValue!=null){
            printer.println("\r\n   - Default:\r\n     " + defaultValue);
        }
        else{
            for (Config c : arr){
                description = config.getDescription();
                printer.println("\r\n   + " + c.getName());
                if (description!=null){
                    printer.println("\r\n      - Description:\r\n     " + description);
                }
            }
        }
    }


  //**************************************************************************
  //** Printer Class
  //**************************************************************************
    private static class Printer {
        private StringBuilder out;
        public Printer(){
            out = new StringBuilder();
        }
        public void println(String line){
            out.append(line);
            out.append("\r\n");
        }
        public void print(String line){
            out.append(line);
        }
        public String toString(){
            return out.toString();
        }
    }


  //**************************************************************************
  //** Word Class
  //**************************************************************************
    private static class Word {
        private String word;
        private int end;

        private String lastComment = null;
        private int lastCommentIndex = -1;

        public Word(String word, int end, String lastComment, int lastCommentIndex){
            this.word = word;
            this.end = end;
            this.lastComment = lastComment;
            this.lastCommentIndex = lastCommentIndex;
        }
        public String toString(){
            return word;
        }

        public Word getNextWord(String s){
            if (end+1>s.length()) return null;
            return Parser.getNextWord(s, end+1);
        }
    }


  //**************************************************************************
  //** Comment Class
  //**************************************************************************
    private static class Comment {
        private String description;
        private ArrayList<String> annotations;
        public Comment(String description, ArrayList<String> annotations){
            this.description = description;
            this.annotations = annotations;
        }
        public String getDescription(){
            return description;
        }
        public ArrayList<String> getAnnotations(){
            return annotations;
        }
    }

}