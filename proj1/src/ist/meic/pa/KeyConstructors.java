package ist.meic.pa;

import javassist.*;
import javassist.compiler.*;

import java.util.*;
import java.util.regex.Pattern;

public class KeyConstructors {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("No class file specified.");
            return;
        }

        ClassPool pool = ClassPool.getDefault();
        List<CtClass> classes = new ArrayList<>();

        for (String arg : args) {
            CtClass cl;

            try {
                cl = pool.get(arg);
            } catch (NotFoundException e) {
                e.printStackTrace(System.err);
                return;
            }

            classes.add(cl);
        }

        for (CtClass cl : classes) {
            handleKeywordArgs(cl);
        }
    }

    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*");

    public class KeywordArguments {
        public Map<String, Object> arguments = new HashMap<>();

        KeywordArguments(String value) {
            List<String> keyVals = new ArrayList<>();
            String[] commaParts = value.split(",");

            for (String commaPart : commaParts) {
                String[] equalsParts = commaPart.split("=", 2);

                if (equalsParts.length == 1)
                    throw new RuntimeException("Must implement merging!");

                String identifier = equalsParts[0], expression = equalsParts[1];
                if (!JAVA_IDENTIFIER.matcher(identifier).matches())
                    throw new RuntimeException("Must implement merging!");

                if (arguments.get(identifier) != null)
                    throw new RuntimeException("Duplicate argument \"" + identifier + "\"");
            }
        }

    }

    private static void handleKeywordArgs(CtClass cl) {
        for (CtConstructor ct : cl.getConstructors()) {
            if (!ct.hasAnnotation(KeywordArgs.class))
                continue;

            float a, b;

            Math.pow(a=3, b=4);

            KeywordArgs annotation;
            try {
                annotation = (KeywordArgs) ct.getAnnotation(KeywordArgs.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }


        }
    }
}
