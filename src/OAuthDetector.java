import config.Constants;
import config.Settings;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

import java.util.Iterator;
import java.util.Map;

public class OAuthDetector {

    private static String apkPath = "";

    public static void main(String[] args){

        // Initialize variables
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-process-dir")) {
                apkPath = args[i + 1];
                break;
            }
        }

        // initialize soot
        Settings.initializeSoot(apkPath);

        // Get the class hierarchy of current application
        Hierarchy classHierarchy = Scene.v().getActiveHierarchy();

        // run Soot to find the oauth providers
        PackManager.v().getPack("jtp").add(new Transform("jtp.OAuthDetector", new BodyTransformer() {
            @Override
            protected void internalTransform(Body b, String s, Map<String, String> map) {
                final PatchingChain<Unit> units = b.getUnits();
                SootClass c = b.getMethod().getDeclaringClass();
                for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
                    final Unit u = iter.next();
                    u.apply(new AbstractStmtSwitch() {

                        public void caseInvokeStmt(InvokeStmt stmt) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            String[] detectionResults = utils.SootExprHandler.handleOAuthInvokeExpr(b, u, c, invokeExpr, classHierarchy);

                        }


                        public void caseAssignStmt(AssignStmt stmt) {
                            Value rightOp = stmt.getRightOp();
                            if (rightOp instanceof InvokeExpr) {
                                String[] detectionResults = utils.SootExprHandler.handleOAuthInvokeExpr(b, u, c, (InvokeExpr) rightOp, classHierarchy);
                            }
                        }
                    });
                }
            }
        }));

        soot.Main.main(args);
    }
}
