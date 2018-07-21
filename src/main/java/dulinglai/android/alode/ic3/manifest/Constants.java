package dulinglai.android.alode.ic3.manifest;

public class Constants {
    public static final class PermissionLevel {
        public static final String NORMAL_SHORT = "n";
        public static final String DANGEROUS_SHORT = "d";
        public static final String SIGNATURE_SHORT = "s";
        public static final String SIGNATURE_OR_SYSTEM_SHORT = "o";
    }

    public static final class ComponentShortType {
        public static final String ACTIVITY = "a";
        public static final String SERVICE = "s";
        public static final String RECEIVER = "r";
        public static final String PROVIDER = "p";
        public static final String DYNAMIC_RECEIVER = "d";
    }

    public static final class ValueLimit {
        public static final int BUNDLE = 20 * 1024;
        public static final int INTENT = 20 * 1024;
        public static final int FILTER = 20 * 1024;
    }

    public static final String ANY_STRING = "(.*)";
    public static final String ANY_CLASS = "<ANY_CLASS>";
    public static final int NOT_FOUND = -1;
}
