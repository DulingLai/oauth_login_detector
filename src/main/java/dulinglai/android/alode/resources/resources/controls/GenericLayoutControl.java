package dulinglai.android.alode.resources.resources.controls;

import soot.SootClass;

import java.util.Map;

/**
 * Generic layout control that can be anything
 * 
 * @author Steven Arzt
 *
 */
public class GenericLayoutControl extends AndroidLayoutControl {

	public GenericLayoutControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
		super(id, viewClass, additionalAttributes);
	}

	public GenericLayoutControl(int id, SootClass viewClass) {
		super(id, viewClass);
	}

	GenericLayoutControl(SootClass viewClass) {
		super(viewClass);
	}

}
