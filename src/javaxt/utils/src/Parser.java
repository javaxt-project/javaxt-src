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

        if (ext==null) throw new IllegalArgumentException("Unknown file type");
        ext = ext.trim().toLowerCase();

        if (!(ext.equals("java") || ext.equals("js")))
            throw new IllegalArgumentException("Unsupported file type: " + ext);


        s = s.replace("\t", "    ").trim();
        s = s.replace("\r\n", "\n").trim();
        s = s.replace("\r", "\n").trim();
        while (s.contains("\n\n")) s = s.replace("\n\n", "\n");
        s = s.trim();

        sourceCode = s;


        if (ext.equals("js")){
            classes = getClasses(s);
            return;
        }

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
                    i = i+quote.length()-1;
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

                            if (word.equals("package")){
                                String[] arr = readFunction(s, i);
                                String fn = arr[0].replace(" ", "");
                                currNamespace = fn;
                            }

                            else if (word.equals("class") || word.equals("interface")){

                              //Get class name and create new Class
                                String[] arr = readFunction(s, i);
                                String fn = arr[0]; //class name followed by extends, implements, etc
                                arr = fn.split(" ");
                                String name = arr[0];
                                Class cls = new Class(name);
                                boolean isInterface = word.equals("interface");
                                cls.setInterface(isInterface);


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
                                    String comment = parseComment(lastComment).getDescription();
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


//                                console.log("-----------------------------");
//                                console.log(modifiers, "class " + "|" + name + "|");
//                                console.log("-----------------------------");
//                                if (cls.isPublic()) console.log(cls.getDescription());
                            }


                            else if (word.equals("public")){ //skipping "private", "protected", etc
                                String[] arr = readFunction(s, i);
                                String fn = arr[0];


                              //If the public member is not a class or an interface...
                                if (!(" " + fn + " ").contains(" class ") &&
                                    !(" " + fn + " ").contains(" interface ")){

                                    Comment comment = null;
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
                                        int end = getEndBacket(s, i, '{');
                                        //console.log(s.substring(i, end));

                                        if (end>-1) i = end;
                                    }
                                    else{

                                        if (lastChar==';' && currClass.isInterface()){

                                          //Parse method and add to class
                                            currClass.addMember(parseMethod(fn, comment, ext));


                                          //Move past the function
                                            int end = i+raw.length()+1;
                                            //console.log(s.substring(i, end));

                                            i = end;
                                        }
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
                        String word;
                        if (idx>-1){
                            word = code.substring(idx);
                        }
                        else{
                            word = code.toString();
                        }


                        if (!word.isBlank()){
                            return new Word(word, i, lastComment, lastCommentIndex);
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
        return new Word(code.toString(), i, lastComment, lastCommentIndex);
    }


  //**************************************************************************
  //** getModifiers
  //**************************************************************************
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


  //**************************************************************************
  //** getEndOfLastComment
  //**************************************************************************
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


  //**************************************************************************
  //** parseMethod
  //**************************************************************************
    private static Method parseMethod(String fn, Comment comment, String ext){


      //Parse comments separating description from annotations
        String description = null;
        ArrayList<String> annotations = new ArrayList<>();
        if (comment!=null){
            description = comment.getDescription();
            annotations = comment.getAnnotations();
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
            int b = -1; //member.indexOf(")");
            int numParentheses = 1;
            for (int i=a+1; i<member.length(); i++){
                char c = member.charAt(i);
                if (c=='(') numParentheses++;
                else if (c==')') numParentheses--;
                if (numParentheses==0){
                    b=i;
                    break;
                }
            }

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
    private static ArrayList<Class> getClasses(String s){
        ArrayList<Class> classes = new ArrayList<>();


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
                        classes.add(cls);


                      //Add constructor
                        Constructor contructor = new Constructor(functionName);
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
  /** Returns the name of a javascript function found in the given word or
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

        s = s.trim();
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length()-1).trim();

        ArrayList<String> params = new ArrayList<>();


        int i=0;
        Word word;
        while ((word = getNextWord(s, i))!=null){

            String str = word.toString().trim();
            if (!str.equals(",")){
                if (str.startsWith(",")) str = str.substring(1).trim();
                if (str.endsWith(",")) str = str.substring(0, str.length()-1);
                if (str.contains(",")){
                    String[] arr = str.split(",");
                    for (String p : arr) params.add(p.trim());
                }
                else{
                    params.add(str.trim());
                }
            }

            i = word.end+1;
        }



        ArrayList<Parameter> parameters = new ArrayList<>();
        for (String param : params){
            Parameter parameter = new Parameter(param);
            for (String annotation : annotations){
                if (annotation.startsWith("@param " + param + " ")){
                    annotation = annotation.substring(("@param " + param).length()+1);
                    parameter.setDescription(annotation);
                    break;
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
                arr.add(config);


              //Get config value and update i
                i = getDefaultValue(word.end, nextWord, config, s);

            }
            else if (str.startsWith(":")){

              //Create config using the previous word
                String key = prevWord.toString().trim();
                Config config = new Config(key);
                config.setDescription(parseComment(prevWord.lastComment).getDescription());
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
    private static void print(javaxt.io.File input) throws Exception {
        ArrayList<Class> classes = new Parser(input).getClasses();

        for (Class cls : classes){
            String className = cls.getName();
            String namespace = cls.getNamespace();
            boolean isInterface = cls.isInterface();
            if (namespace!=null) className = namespace + "." + className;
            System.out.println("-----------------------------------");
            System.out.println("- " + className + (isInterface? " (Interface)" : ""));
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
                System.out.println("\r\n## Constructors: ");
                for (Constructor c : contructors){
                    printMethod(c);
                }
            }


            ArrayList<Config> config = cls.getConfig();
            if (!config.isEmpty()){
                System.out.println("\r\n## Config: ");
                for (Config c : config){
                    printConfig(c);
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


  //**************************************************************************
  //** printConfig
  //**************************************************************************
    private static void printConfig(Config config){
        String name = config.getName();
        String description = config.getDescription();
        String defaultValue = config.getDefaultValue();
        ArrayList<Config> arr = config.getConfig();

        if (!arr.isEmpty()) defaultValue = null;



        System.out.println("\r\n+ " + name);
        if (description!=null){
            System.out.println("\r\n   - Description:\r\n     " + description);
        }

        if (defaultValue!=null){
            System.out.println("\r\n   - Default:\r\n     " + defaultValue);
        }
        else{
            for (Config c : arr){
                description = config.getDescription();
                System.out.println("\r\n   + " + c.getName());
                if (description!=null){
                    System.out.println("\r\n      - Description:\r\n     " + description);
                }
            }
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