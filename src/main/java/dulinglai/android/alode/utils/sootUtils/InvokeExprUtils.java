package dulinglai.android.alode.utils.sootUtils;

import org.pmw.tinylog.Logger;
import soot.jimple.InvokeExpr;

public class InvokeExprUtils {

    private static final String START_ACTIVITY_METHODS = "startActivity";

    public static boolean invokesStartActivity(InvokeExpr inv) {
        if (inv.getMethod().getName().equalsIgnoreCase(START_ACTIVITY_METHODS)) {
            Logger.debug("Here");
            return true;
        } else {
            return false;
        }
    }
}
