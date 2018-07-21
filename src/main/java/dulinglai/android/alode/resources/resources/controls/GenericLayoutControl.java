package dulinglai.android.alode.resources.resources.controls;

import java.util.Collections;
import java.util.Map;

import soot.SootClass;
import dulinglai.android.alode.sourcesink.AccessPathTuple;
import dulinglai.android.alode.sourcesink.MethodSourceSinkDefinition;
import dulinglai.android.alode.sourcesink.MethodSourceSinkDefinition.CallType;
import dulinglai.android.alode.sourcesink.SourceSinkDefinition;
import dulinglai.android.alode.sourcesink.SourceSinkType;

/**
 * Generic layout control that can be anything
 * 
 * @author Steven Arzt
 *
 */
public class GenericLayoutControl extends AndroidLayoutControl {

	protected final static SourceSinkDefinition UI_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
			Collections.singleton(AccessPathTuple.fromPathElements(Collections.singletonList("content"),
					Collections.singletonList("java.lang.Object"), SourceSinkType.Source)),
			CallType.MethodCall);

	public GenericLayoutControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
		super(id, viewClass, additionalAttributes);
	}

	public GenericLayoutControl(int id, SootClass viewClass) {
		super(id, viewClass);
	}

	public GenericLayoutControl(SootClass viewClass) {
		super(viewClass);
	}

	@Override
	public SourceSinkDefinition getSourceDefinition() {
		return UI_SOURCE_DEF;
	}

}
