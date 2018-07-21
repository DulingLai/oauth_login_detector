package dulinglai.android.alode.sootData;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Tag for marking a method as essential, i.e., data flows must always be
 * tracked through this method, even if its parent class is a system class
 * or excluded by some other means.
 *
 * @author Steven Arzt
 *
 */
public class EssentialMethodTag implements Tag {

    public static final String TAG_NAME = "fd_essential_method";

    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }

}
