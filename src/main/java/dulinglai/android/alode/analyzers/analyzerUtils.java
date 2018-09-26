package dulinglai.android.alode.analyzers;

import dulinglai.android.alode.graphBuilder.widgetNodes.ClickWidgetNode;
import dulinglai.android.alode.utils.sootUtils.SootMethodRepresentationParser;
import heros.solver.Pair;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class analyzerUtils {

    /**
     * Checks if the method invocation is a wrapper method for findViewById
     * @param sm The method invocation
     * @return True if the method invocation is a wrapper method for findViewById
     */
    static boolean isWrapperForFindViewById(SootMethod sm) {
        if (sm.getReturnType().toString().equalsIgnoreCase("android.view.View")) {
            int i = 0;
            for (Type paramType : sm.getParameterTypes()) {
                if (paramType.toString().equals("int")) {
                    Local paramLocal = sm.retrieveActiveBody().getParameterLocal(i);
                    // Check the usage of this local variable
                    BriefUnitGraph unitGraph = new BriefUnitGraph(sm.retrieveActiveBody());
                    SimpleLocalDefs localDefs = new SimpleLocalDefs(unitGraph);
                    SimpleLocalUses localUses = new SimpleLocalUses(unitGraph, localDefs);

                    List<Unit> intDefs = localDefs.getDefsOf(paramLocal);
                    for (Unit intDef : intDefs) {
                        List<UnitValueBoxPair> usePair = localUses.getUsesOf(intDef);
                        if (usePair.size()>0) {
                            Unit u = usePair.get(0).getUnit();
                            if (u instanceof Stmt) {
                                Stmt stmt = (Stmt) u;
                                if (stmt.containsInvokeExpr()) {
                                    InvokeExpr inv = stmt.getInvokeExpr();
                                    if (invokesFindViewById(inv)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                i++;
            }
        }
        return false;
    }

    /**
     * Checks whether this invocation calls Android's findViewById method
     * @param inv The invocaton to check
     * @return True if this invocation calls findViewById, otherwise false
     */
    static boolean invokesFindViewById(InvokeExpr inv) {
        String methodName = SootMethodRepresentationParser.v()
                .getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
        String returnType = inv.getMethod().getReturnType().toString();

        return returnType.equalsIgnoreCase("android.view.View") &&
                methodName.equalsIgnoreCase("findViewById");

    }


    /**
     * Check if the value is reassigned to another local
     * @param usePair The use pair to check for reassignment
     * @return The local the was reassigned, otherwise null
     */
    static Pair<Local, Unit> reassignsLocal(List<UnitValueBoxPair> usePair){
        for (UnitValueBoxPair anUsePair : usePair) {
            Unit useUnit = anUsePair.getUnit();

            if (useUnit instanceof Stmt) {
                Stmt newStmt = (Stmt) useUnit;
                if (newStmt instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) newStmt;
                    Value newRightOp = assignStmt.getRightOp();

                    if (newRightOp instanceof CastExpr) {
                        Value leftOp = assignStmt.getLeftOp();
                        if (leftOp instanceof Local) {
                            return new Pair<>((Local) leftOp, useUnit);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the click widgets with given click listener
     * @param clickListener The click listener to look for
     * @param widgetNodeList The list of widgets to look through
     * @return A set of widgets with the given click listener
     */
    static Set<ClickWidgetNode> findWidgetsWithClickListener(String clickListener,
                                                                List<ClickWidgetNode> widgetNodeList) {
        Set<ClickWidgetNode> clickWidgetSet = new HashSet<>();
        for (ClickWidgetNode clickWidgetNode : widgetNodeList) {
            if (clickWidgetNode.getClickListener().equals(clickListener))
                clickWidgetSet.add(clickWidgetNode);
        }
        return clickWidgetSet;
    }
}
