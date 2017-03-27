package ist.meic.pa;

import javassist.compiler.CompileError;
import javassist.compiler.TokenId;
import javassist.compiler.ast.*;

class AstToJava extends Visitor implements TokenId {
    StringBuilder sb = new StringBuilder();
    String listSep = "";

    @Override
    public void atASTList(ASTList n) throws CompileError {
        if (n.getLeft() != null) {
            n.getLeft().accept(this);

            if (n.getRight() != null)
                sb.append(listSep);
        }

        if (n.getRight() != null)
            n.getRight().accept(this);
    }

    @Override
    public void atAssignExpr(AssignExpr n) throws CompileError {
        n.getLeft().accept(this);
        sb.append("=");
        n.getRight().accept(this);
    }

    @Override
    public void atCondExpr(CondExpr n) throws CompileError {
        sb.append("(");
        n.condExpr().accept(this);
        sb.append(")?(");
        n.thenExpr().accept(this);
        sb.append("):(");
        n.elseExpr().accept(this);
        sb.append(")");
    }

    @Override
    public void atBinExpr(BinExpr n) throws CompileError {
        sb.append("(");
        n.getLeft().accept(this);
        sb.append(")" + n.getName() + "(");
        n.getRight().accept(this);
        sb.append(")");
    }

    @Override
    public void atExpr(Expr n) throws CompileError {
        n.getLeft().accept(this);
        sb.append(n.getName());
        n.getRight().accept(this);
    }

    @Override
    public void atCallExpr(CallExpr n) throws CompileError {
        n.getLeft().accept(this);
        sb.append("(");
        listSep = ",";
        n.getRight().accept(this);
        listSep = "";
        sb.append(")");
    }

    @Override
    public void atCastExpr(CastExpr n) throws CompileError {
        sb.append("(");
        n.getLeft().accept(this);
        sb.append(")");
        sb.append("(");
        n.getRight().accept(this);
        sb.append(")");
    }

    @Override
    public void atInstanceOfExpr(InstanceOfExpr n) throws CompileError {
        sb.append("(");
        n.getLeft().accept(this);
        sb.append(") instanceof ");
        n.getRight().accept(this);
        throw new UnsupportedOperationException();
    }

    @Override
    public void atNewExpr(NewExpr n) throws CompileError {
        sb.append("new ");
        listSep = ".";
        n.getLeft().accept(this);
        listSep = "";

        sb.append("(");
        listSep = ",";
        n.getRight().accept(this);
        listSep = "";
        sb.append(")");
    }

    @Override
    public void atSymbol(Symbol n) throws CompileError {
        sb.append(n.get());
    }

    @Override
    public void atMember(Member n) throws CompileError {
        sb.append(n.toString());
    }

    @Override
    public void atVariable(Variable n) throws CompileError {
        sb.append(n.toString());
    }

    @Override
    public void atKeyword(Keyword n) throws CompileError {
        switch (n.get()) {
        case TRUE:
            sb.append("true");
            break;
        case FALSE:
            sb.append("false");
            break;
        case NULL:
            sb.append("null");
            break;
        default:
            throw new RuntimeException("unexpected keyword");
        }
    }

    @Override
    public void atStringL(StringL n) throws CompileError {
        sb.append(n.toString());
    }

    @Override
    public void atIntConst(IntConst n) throws CompileError {
        sb.append(n.toString());
    }

    @Override
    public void atDoubleConst(DoubleConst n) throws CompileError {
        sb.append(n.toString());
    }

    @Override
    public void atArrayInit(ArrayInit n) throws CompileError {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
