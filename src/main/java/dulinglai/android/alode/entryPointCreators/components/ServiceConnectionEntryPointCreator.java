package dulinglai.android.alode.entryPointCreators.components;

import dulinglai.android.alode.resources.androidConstants.ComponentConstants;
import soot.SootClass;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;

/**
 * Entry point creator for Android service connections
 * 
 * @author Steven Arzt
 *
 */
public class ServiceConnectionEntryPointCreator extends AbstractComponentEntryPointCreator {

	public ServiceConnectionEntryPointCreator(SootClass component, SootClass applicationClass) {
		super(component, applicationClass);
	}

	@Override
	protected void generateComponentLifecycle() {
		searchAndBuildMethod(ComponentConstants.SERVICECONNECTION_ONSERVICECONNECTED, component, thisLocal);

		// methods
		NopStmt startWhileStmt = Jimple.v().newNopStmt();
		NopStmt endWhileStmt = Jimple.v().newNopStmt();
		body.getUnits().add(startWhileStmt);
		createIfStmt(endWhileStmt);
		addCallbackMethods();
		body.getUnits().add(endWhileStmt);
		createIfStmt(startWhileStmt);

		searchAndBuildMethod(ComponentConstants.SERVICECONNECTION_ONSERVICEDISCONNECTED, component, thisLocal);
	}

}
