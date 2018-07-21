package dulinglai.android.alode.callbacks.filters;

import dulinglai.android.alode.callbacks.ComponentReachableMethods;

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
