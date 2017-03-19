package ist.meic.pa;

import javassist.*;
import javassist.compiler.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public class KeywordArguments {
        public Map<String, Object> arguments = new HashMap<>();

        KeywordArguments(String value) {
            Lex lex = new Lex(value);
            Parser parser = new Parser(lex);
        }
    }

    private static void handleKeywordArgs(CtClass cl) {
        for (CtConstructor ct : cl.getConstructors()) {
            if (!ct.hasAnnotation(KeywordArgs.class))
                continue;

            KeywordArgs annotation;
            try {
                annotation = (KeywordArgs)ct.getAnnotation(KeywordArgs.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }


        }
    }
}
