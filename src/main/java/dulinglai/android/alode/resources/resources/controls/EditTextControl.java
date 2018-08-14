package dulinglai.android.alode.resources.resources.controls;

import dulinglai.android.alode.resources.axml.AXmlAttribute;
import pxb.android.axml.AxmlVisitor;
import soot.SootClass;

import java.util.Map;

/**
 * EditText control in Android
 *
 * @author Steven Arzt
 *
 */
public class EditTextControl extends AndroidLayoutControl {

    private final static int TYPE_CLASS_TEXT = 0x00000001;
    private final static int TYPE_CLASS_NUMBER = 0x00000002;
    private final static int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
    private final static int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
    private final static int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
    private final static int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;

    private int inputType;
    private boolean isPassword;
    private String contentDescription;
    private String hint;

    EditTextControl(SootClass viewClass) {
        super(viewClass);
    }

    public EditTextControl(int id, SootClass viewClass) {
        super(id, viewClass);
    }

    public EditTextControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
        super(id, viewClass, additionalAttributes);
    }

    /**
     * Sets the type of this input (text, password, etc.)
     *
     * @param inputType
     *            The input type
     */
    void setInputType(int inputType) {
        this.inputType = inputType;
    }

    /**
     * Gets the type of this input (text, password, etc.)
     *
     * @return The input type
     */
    public int getInputType() {
        return inputType;
    }

    /**
     * Gets the text of this edit control
     *
     * @return The text of this edit control
     */
    public String getText() {
        return text;
    }

    @Override
    protected void handleAttribute(AXmlAttribute<?> attribute, boolean loadOptionalData) {
        final String attrName = attribute.getName().trim();
        final int type = attribute.getType();

        // Collect attributes of this widget
        if (attrName.equals("id")) {
            if (type == AxmlVisitor.TYPE_INT_HEX)
                id = (Integer) attribute.getValue();
        }
        else if (attrName.equals("inputType") && type == AxmlVisitor.TYPE_INT_HEX) {
            inputType = (Integer) attribute.getValue();
        }
        else if (attrName.equals("password")) {
            if (type == AxmlVisitor.TYPE_INT_HEX)
                isPassword = ((Integer) attribute.getValue()) != 0; // -1 for
                // true, 0
                // for false
            else if (type == AxmlVisitor.TYPE_INT_BOOLEAN)
                isPassword = (Boolean) attribute.getValue();
            else
                throw new RuntimeException("Unknown representation of boolean data type");
        }
        else if (attrName.equals("text")) {
            if (type == AxmlVisitor.TYPE_INT_HEX) {
                text = String.valueOf(attribute.getValue());
            } else if (type == AxmlVisitor.TYPE_STRING)
                text = (String) attribute.getValue();
        }
        else if (attrName.equals("contentDescription")) {
            if (type == AxmlVisitor.TYPE_INT_HEX)
                contentDescription = String.valueOf(attribute.getValue());
            else if (type == AxmlVisitor.TYPE_STRING)
                contentDescription = (String) attribute.getValue();
        }
        else if (attrName.equals("hint")){
            if (type == AxmlVisitor.TYPE_INT_HEX)
                hint = String.valueOf(attribute.getValue());
            else if (type == AxmlVisitor.TYPE_STRING)
                hint = (String) attribute.getValue();
        }
        else
            super.handleAttribute(attribute, loadOptionalData);
    }

    @Override
    public boolean isSensitive() {
        if (isPassword)
            return true;

        if ((inputType & TYPE_CLASS_NUMBER) == TYPE_CLASS_NUMBER)
            return ((inputType & TYPE_NUMBER_VARIATION_PASSWORD) == TYPE_NUMBER_VARIATION_PASSWORD);

        if ((inputType & TYPE_CLASS_TEXT) == TYPE_CLASS_TEXT) {
            return ((inputType & TYPE_TEXT_VARIATION_PASSWORD) == TYPE_TEXT_VARIATION_PASSWORD)
                    || ((inputType & TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    || ((inputType & TYPE_TEXT_VARIATION_WEB_PASSWORD) == TYPE_TEXT_VARIATION_WEB_PASSWORD);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + inputType;
        result = prime * result + (isPassword ? 1231 : 1237);
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        EditTextControl other = (EditTextControl) obj;
        if (inputType != other.inputType)
            return false;
        if (isPassword != other.isPassword)
            return false;
        if (text == null) {
            return other.text == null;
        } else return text.equals(other.text);
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public String getHint() {
        return hint;
    }
}
