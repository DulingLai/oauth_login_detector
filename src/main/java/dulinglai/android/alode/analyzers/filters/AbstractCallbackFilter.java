package dulinglai.android.alode.analyzers.filters;

import dulinglai.android.alode.analyzers.ComponentReachableMethods;

/**
 * Abstract base class for callback filters
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractCallbackFilter implements ICallbackFilter {

	protected ComponentReachableMethods reachableMethods = null;

	@Override
	public void setReachableMethods(ComponentReachableMethods rm) {
		this.reachableMethods = rm;
	}

}
