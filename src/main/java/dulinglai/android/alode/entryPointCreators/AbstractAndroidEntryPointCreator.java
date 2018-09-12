package dulinglai.android.alode.entryPointCreators;

import dulinglai.android.alode.resources.androidConstants.ComponentConstants;
import dulinglai.android.alode.utils.androidUtils.ClassUtils;
import dulinglai.android.alode.utils.androidUtils.SystemClassHandler;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public abstract class AbstractAndroidEntryPointCreator extends BaseEntryPointCreator {

    protected ClassUtils entryPointUtils = null;

    @Override
    public SootMethod createDummyMain() {
        // Initialize the utility class
        this.entryPointUtils = new ClassUtils();

        return super.createDummyMain();
    }

    protected Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal) {
        return searchAndBuildMethod(subsignature, currentClass, classLocal, Collections.emptySet());
    }

    protected Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal,
                                        Set<SootClass> parentClasses) {
        if (currentClass == null || classLocal == null)
            return null;

        SootMethod method = findMethod(currentClass, subsignature);
        if (method == null) {
            logger.warn("Could not find Android entry point method: {}", subsignature);
            return null;
        }

        // If the method is in one of the predefined Android classes, it cannot
        // contain custom code, so we do not need to call it
        if (ComponentConstants.isLifecycleClass(method.getDeclaringClass().getName()))
            return null;

        // If this method is part of the Android framework, we don't need to
        // call it
        if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
            return null;

        assert method.isStatic() || classLocal != null : "Class local was null for non-static method "
                + method.getSignature();

        // write Method
        return buildMethodCall(method, mainMethod.getActiveBody(), classLocal, generator, parentClasses);
    }

    protected boolean createPlainMethodCall(Local classLocal, SootMethod currentMethod) {
        // Do not create calls to lifecycle methods which we handle explicitly
        if (ComponentConstants.getServiceLifecycleMethods().contains(currentMethod.getSubSignature()))
            return false;

        NopStmt beforeStmt = Jimple.v().newNopStmt();
        NopStmt thenStmt = Jimple.v().newNopStmt();
        body.getUnits().add(beforeStmt);
        createIfStmt(thenStmt);
        buildMethodCall(currentMethod, body, classLocal, generator);

        body.getUnits().add(thenStmt);
        createIfStmt(beforeStmt);
        return true;
    }

    public void setEntryPointUtils(ClassUtils entryPointUtils) {
        this.entryPointUtils = entryPointUtils;
    }

    /**
     * Creates instance of the given classes
     *
     * @param classes The classes of which to create instances
     */
    protected void createClassInstances(Collection<SootClass> classes) {
        for (SootClass callbackClass : classes) {
            NopStmt thenStmt = Jimple.v().newNopStmt();
            createIfStmt(thenStmt);
            Local l = localVarsForClasses.get(callbackClass);
            if (l == null) {
                l = generateClassConstructor(callbackClass, body);
                if (l != null)
                    localVarsForClasses.put(callbackClass, l);
            }
            body.getUnits().add(thenStmt);
        }
    }

}
