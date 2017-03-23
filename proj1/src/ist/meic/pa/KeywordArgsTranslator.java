package ist.meic.pa;

import javassist.*;
import javassist.compiler.CompileError;
import javassist.compiler.Lex;
import javassist.compiler.Parser;
import javassist.compiler.SymbolTable;
import javassist.compiler.ast.*;

import java.util.*;

public class KeywordArgsTranslator implements Translator {
    @Override
    public void start(ClassPool classPool) throws NotFoundException, CannotCompileException {

    }

    private final static String INVALID_EXPRESSION = "-";

    @Override
    public void onLoad(ClassPool classPool, String s) throws NotFoundException, CannotCompileException {
        CtClass cl = classPool.get(s);

        final CtClass OBJECT_ARRAY = classPool.get("java.lang.Object[]");

        for (CtConstructor ct : cl.getConstructors()) {
            if (!ct.hasAnnotation(KeywordArgs.class))
                continue;

            if (ct.getParameterTypes().length != 1 || !OBJECT_ARRAY.equals(ct.getParameterTypes()[0]))
                throw new RuntimeException("Can only apply keyword arguments to constructors that take object arrays!");

            KeywordArgs ka = null;
            try {
                 ka = (KeywordArgs)ct.getAnnotation(KeywordArgs.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

            handleConstructor(cl, ct, ka);
        }
    }

    private void handleConstructor(CtClass cl, CtConstructor ct, KeywordArgs ka) {
        Map<String, String> keywordArgs = parseKeywordArgs(ka.value());

        Set<String> fieldNames = new HashSet<>(cl.getFields().length);
        for (CtField field : cl.getFields())
            fieldNames.add(field.getName());

        Set<String> keywordArgsNames = new HashSet<>(keywordArgs.keySet());
        keywordArgsNames.removeAll(fieldNames);

        if (keywordArgsNames.size() != 0)
            throw new RuntimeException(String.format("keyword argument '%s' not a member of class '%s'",
                    keywordArgsNames.iterator().next(),
                    cl.toString()));

        // TODO: do the keyword arguments dance

    }

    private Map<String, String> parseKeywordArgs(String value) {
        // I don't know (or care) if we can pass null to the parser, so construct a dummy SymbolTable.
        SymbolTable st = new SymbolTable();

        // These do all the hard work of lexing and parsing for us.
        Lex lex = new Lex(value);
        Parser parser = new Parser(lex);

        Map<String, String> keyargs = new HashMap<>();

        // XXX: Fragile. This API doesn't seem to be documented (: Javassist uses it, and now so do I.
        // Also, I use exceptions as control flow. You can spank me later for being a bad boy.
        try {
            while (parser.hasMore()) {
                // Internally this walks through the string.
                ASTree ast = parser.parseExpression(st);

                String variable, expression = INVALID_EXPRESSION;
                if (ast instanceof AssignExpr) {
                    AssignExpr assign = (AssignExpr)ast;

                    if (assign.getLeft() instanceof Member || assign.getLeft() instanceof Variable)
                        variable = ((Symbol)assign.getLeft()).get();
                    else
                        throw new CompileError("expected variable", lex);

                    // XXX: I have to stringify the AST... is there a better way?
                    AstToJava atj = new AstToJava();
                    ast.getRight().accept(atj);

                    expression = atj.toString();
                } else if (ast instanceof Member || ast instanceof Variable) {
                    variable = ((Symbol)ast).get();
                } else
                    throw new CompileError("expected assignment or variable", lex);

                if (keyargs.put(variable, expression) != null)
                    throw new CompileError("duplicate argument '" + variable + "'", lex);

                // Lexer.get returns a tokenID, whatever that is. What's useful to me is that it returns ASCII when
                // it lexes a single ASCII character. It also returns <0 if there's no more input, but that is
                // abstracted by Parser.hasMore, perhaps in more ways than I have thought about.
                if (lex.get() != (int)',' && parser.hasMore())
                    throw new CompileError("expected ','", lex);
            }
        } catch (CompileError cex) {
            System.err.printf("Syntax error: %s\n", cex.getMessage());
            System.exit(1);
        }

        return keyargs;
    }
}
