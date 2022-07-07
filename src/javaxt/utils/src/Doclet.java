package javaxt.utils.src;

import java.util.*;
import javax.lang.model.SourceVersion;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;


//******************************************************************************
//**  Doclet Class
//******************************************************************************
/**
 *   Custom doclet implementation that uses the Parser class instead of the
 *   native java libraries to parse source files.
 *
 ******************************************************************************/

public class Doclet implements jdk.javadoc.doclet.Doclet {
    private String fileName;
    private String directory;
        
    private boolean processArg(String opt, List<String> arguments){
        if (opt.equals("-d")) directory = arguments.get(0);
        if (opt.equals("-filename")) fileName = arguments.get(0);
        return true;
    }
    
    
    @Override
    public void init(Locale locale, Reporter reporter) {
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        //System.out.println("directory: " + directory);
        //System.out.println("fileName: " + fileName);
        

        HashSet<javaxt.io.File> files = new HashSet<>();
        Iterator it = docEnv.getIncludedElements().iterator();
        while (it.hasNext()){
            Object obj = it.next(); //com.sun.tools.javac.code.Symbol$ClassSymbol

          //Get classname
            String className = obj.toString();
            
            
          //Get path
            String path = null;
            try{
                java.lang.reflect.Field field;
                Object f;
                
              //Get sourcefile (DirectoryFileObject)
                Object sourceFile = getFieldValue("sourcefile", obj);
                //System.out.println("sourcefile: " + sourceFile);
                
                
              //Get base path
                Object basePath = getFieldValue("userPackageRootDir", sourceFile);
                //System.out.println("userPackageRootDir: " + basePath);
                javaxt.io.Directory dir = new javaxt.io.Directory(basePath.toString());
                
                
              //Get relative path
                Object relativePath = getFieldValue("relativePath", sourceFile);
                //System.out.println("relativePath: " + relativePath);
                field = relativePath.getClass().getSuperclass().getDeclaredField("path");
                field.setAccessible(true);
                f = field.get(relativePath);
                //System.out.println("path: " + f);
                path = dir + f.toString();
                files.add(new javaxt.io.File(path));
            }
            catch(Throwable e){
            }
            
            
//            if (path!=null){
//                System.out.println(className);
//                System.out.println("path: " + path);
//                System.out.println();
//            }
        }
        
        for (javaxt.io.File file : files){
            //System.out.println(file);
            try{
                
                ArrayList<Class> classes = new Parser(file).getClasses();
                for (Class c : classes){
                    //System.out.println(" - " + c.getName());
                }
            }
            catch(Exception e){
                System.err.println("Failed to parse " + file.getName(false));
            }
        }
        
        return true;
    }
    
    
    private Object getFieldValue(String fieldName, Object obj) throws Exception {
        java.lang.reflect.Field field;
        field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    @Override
    public String getName() {
        return "JavaXT Doclet";
    }
    
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        HashSet<Option> options = new HashSet<>();
        options.add(createOption("-filename"));
        options.add(createOption("-d"));
        return options;
    }    
    
    private Option createOption(String key){
        return new Option() {
            private final List<String> keys = Arrays.asList(
                key
            );

            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return "an option with aliases";
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return keys;
            }

            @Override
            public String getParameters() {
                return "file";
            }

            @Override
            public boolean process(String opt, List<String> arguments) {
                return processArg(opt, arguments);
            }
        };
    }
}