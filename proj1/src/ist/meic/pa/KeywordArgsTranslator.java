package ist.meic.pa;

import javassist.*;
import javassist.compiler.CompileError;
import javassist.compiler.Lex;
import javassist.compiler.Parser;
import javassist.compiler.SymbolTable;
import javassist.compiler.ast.*;

import java.util.*;
import java.util.function.BiConsumer;

public class KeywordArgsTranslator implements Translator {
    @Override
    public void start(ClassPool classPool) throws NotFoundException, CannotCompileException {

    }

    private final static String INHERITED_VALUE = "";

    @Override
    public void onLoad(ClassPool classPool, String s) throws NotFoundException, CannotCompileException {
        CtClass cl = classPool.get(s);

        onKeywordArgsConstructor(cl, (CtConstructor ct, KeywordArgs ka) -> {
            // I have to do this. I cannot handle this exception here, and since the checked exception is declared to be
            // thrown inside a lambda, it does *not* propagate into the method's signature; so I must catch it here and
            // convert it into a RuntimeException. This interaction is really badly designed. Whoever designed it should
            // be whacked in the head with a damn chair.
            try {
                addDefaultConstructor(cl);
                handleConstructor(cl, ct, ka);
            } catch (CannotCompileException | NotFoundException e) {
                throw new RuntimeException("Java is a fucking pain in the ass", e);
            }
        });
    }

    private void addDefaultConstructor(CtClass cl) throws CannotCompileException {
        CtConstructor ct;

        try {
            // There is a default constructor, we have nothing to do here.
            ct = cl.getConstructor("<init>()V");
            return;
        } catch (NotFoundException _) {
            // This is no exception, we *are* expecting this, so do nothing. We are adding a new one.
        }

        ct = new CtConstructor(new CtClass[0], cl);
        assert "<init>()V".equals(ct.getGenericSignature());

        StringBuilder ctorBody = new StringBuilder();

        ctorBody.append("{\n");
        ctorBody.append("    this(new java.lang.Object[0]);\n");
        ctorBody.append("}\n");

        System.err.println("<" + cl.getName() + "> generated default constructor body:");
        System.err.println(ctorBody);

        ct.setBody(ctorBody.toString());
        cl.addConstructor(ct);
    }

    private void onKeywordArgsConstructor(CtClass cl, BiConsumer<? super CtConstructor, ? super KeywordArgs> func) throws NotFoundException, CannotCompileException {
        Objects.requireNonNull(func);
        boolean hasOneConstructor = false;

        for (CtConstructor ct : cl.getConstructors()) {
            if (!ct.hasAnnotation(KeywordArgs.class))
                continue;

            final CtClass OBJECT_ARRAY = cl.getClassPool().get("java.lang.Object[]");

            if (ct.getParameterTypes().length != 1 || !OBJECT_ARRAY.equals(ct.getParameterTypes()[0]))
                throw new RuntimeException("Can only apply keyword arguments to constructors that take object arrays!");

            KeywordArgs ka = null;
            try {
                ka = (KeywordArgs) ct.getAnnotation(KeywordArgs.class);
            } catch (ClassNotFoundException e) {
                // This should never happen...
                e.printStackTrace(System.err);
                System.exit(1);
            }

            assert !hasOneConstructor;
            func.accept(ct, ka);
            hasOneConstructor = true;
        }
    }

    private void handleConstructor(CtClass cl, CtConstructor ct, KeywordArgs ka) throws CannotCompileException, NotFoundException {
        Map<String, String> keywordArgs = parseKeywordArgs(ka.value());

        checkInvalidParams(cl, keywordArgs);

        StringBuilder methodBody = new StringBuilder();
        methodBody.append("{\n");
        // methodBody.append("    super();\n");

        // We start by simply setting the fields' default values. It is simple to do, easy to optimize.
        keywordArgs.forEach((field, expr) -> {
            if (!INHERITED_VALUE.equals(expr))
                methodBody.append("    " + field + " = (" + expr + ");\n");
            else
                /* if no default value is specified, it receives the default Java value */;
        });

        inheritDefaultValues(cl, keywordArgs);

        methodBody.append("\n");
        methodBody.append("    if ($1.length % 2 != 0)\n");
        methodBody.append("        throw new RuntimeException(\"uneven number of arguments!\");\n");
        methodBody.append("\n");
        methodBody.append("    for (int ix = 0; ix < $1.length; ++ix) {\n");



        methodBody.append("    }\n");
        methodBody.append("}\n");

        System.err.println("<" + cl.getName() + "> generated keyword arguments constructor body:");
        System.err.println(methodBody);

        ct.setBody(methodBody.toString());
        //cl.addConstructor(ct);
    }

    private void inheritDefaultValues(CtClass cl, final Map<String, String> keywordArgs) throws NotFoundException, CannotCompileException {
        final Map<String, String> needDefault = new HashMap<>();
        CtClass parentClass = cl;

        while (parentClass.getSuperclass() != null) {
            needDefault.clear();
            parentClass = parentClass.getSuperclass();

            keywordArgs.forEach((field, expr) -> {
                if (INHERITED_VALUE.equals(expr))
                    needDefault.put(field, expr);
            });

            if (needDefault.size() == 0)
                return;

            onKeywordArgsConstructor(parentClass, (CtConstructor ct, KeywordArgs ka) -> {
                final Map<String, String> keyArgs = parseKeywordArgs(ka.value());

                needDefault.forEach((field, expr) -> {
                    String inheritedDefaultValue = keyArgs.get(field);

                    if (inheritedDefaultValue != null && !INHERITED_VALUE.equals(inheritedDefaultValue))
                        keywordArgs.put(field, inheritedDefaultValue);
                });
            });
        }
    }

    private void checkInvalidParams(CtClass cl, Map<String, String> keywordArgs) throws CannotCompileException {
        // Why does Java suck so bad? Why are arrays not treated as proper collections?
        Set<String> fieldNames = new HashSet<>(cl.getFields().length);
        for (CtField field : cl.getFields())
            fieldNames.add(field.getName());

        // I want to know if there is any keyword arguments that is not a member of the class or any superclass
        Set<String> keywordArgsNames = new HashSet<>(keywordArgs.keySet());
        keywordArgsNames.removeAll(fieldNames);

        if (keywordArgsNames.size() != 0)
            throw new CannotCompileException(String.format("keyword argument '%s' not a member of class '%s'",
                    keywordArgsNames.iterator().next(),
                    cl.toString()));
    }

    private Map<String, String> parseKeywordArgs(String value) {
        // I don't know (or care) if we can pass null to the parser, so construct a dummy SymbolTable.
        SymbolTable st = new SymbolTable();

        // These do all the hard work of lexing and parsing for us.
        Lex lex = new Lex(value);
        Parser parser = new Parser(lex);

        Map<String, String> keyArgs = new HashMap<>();

        // XXX: Fragile. This API doesn't seem to be documented (: Javassist uses it, and now so do I.
        // Also, I use exceptions as control flow. You can spank me later for being a bad boy.
        try {
            while (parser.hasMore()) {
                // Internally this walks through the string.
                ASTree ast = parser.parseExpression(st);

                String variable, expression = INHERITED_VALUE;
                if (ast instanceof AssignExpr) {
                    AssignExpr assign = (AssignExpr) ast;

                    if (assign.getLeft() instanceof Member || assign.getLeft() instanceof Variable)
                        variable = ((Symbol) assign.getLeft()).get();
                    else
                        throw new CompileError("expected variable", lex);

                    // XXX: Since I can't find a way to get the bounds on the parsed string, I have to stringify it...
                    // Is there a better way to do this?
                    AstToJava atj = new AstToJava();
                    ast.getRight().accept(atj);

                    expression = atj.toString();
                } else if (ast instanceof Member || ast instanceof Variable) {
                    variable = ((Symbol) ast).get();
                } else
                    throw new CompileError("expected assignment or variable", lex);

                if (keyArgs.put(variable, expression) != null)
                    throw new CompileError("duplicate argument '" + variable + "'", lex);

                // Lexer.get returns a tokenID, whatever that is. What's useful to me is that it returns ASCII when
                // it lexes a single ASCII character. It also returns <0 if there's no more input, but that is
                // abstracted by Parser.hasMore, perhaps in more ways than I have thought about.
                if (lex.get() != (int) ',' && parser.hasMore())
                    throw new CompileError("expected ','", lex);
            }
        } catch (CompileError cex) {
            System.err.printf("Syntax error: %s\n", cex.getMessage());
            System.exit(1);
        }

        return keyArgs;
    }
}
