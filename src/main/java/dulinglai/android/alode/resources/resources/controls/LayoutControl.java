package dulinglai.android.alode.resources.resources.controls;

/**
 * Abstract base class for all layout controls
 * 
 * @author Steven Arzt
 *
 */
public abstract class LayoutControl {

	/**
	 * Gets whether this control can contain sensitive data according to some
	 * reasonable general definition
	 * 
	 * @return True if this control can contain sensitive information, otherwise
	 *         false
	 */
	public boolean isSensitive() {
		return false;
	}

}
