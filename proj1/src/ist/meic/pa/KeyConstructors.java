package ist.meic.pa;

import javassist.*;

import java.util.*;

public class KeyConstructors {
    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            System.err.println("No class file specified.");
            return;
        }

        // Setup our class loader so we can transform the keyword args into Proper Java™ at load time
        Translator translator = new KeywordArgsTranslator();
        ClassPool pool = ClassPool.getDefault();
        Loader classLoader = new Loader();
        classLoader.addTranslator(pool, translator);

        String[] runArgs = Arrays.copyOfRange(args, 1, args.length);
        classLoader.run(args[0], runArgs);
    }
}
