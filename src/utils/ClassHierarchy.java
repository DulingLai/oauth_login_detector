package utils;

import com.google.common.collect.Maps;
import soot.Scene;
import soot.SootClass;

import java.util.Map;
import java.util.Set;

/*
found a new soot implementation, not using this hierarchy anymore
 */

public class ClassHierarchy {

    // -------------------------------------------------------------
    // this hash table contains a pair (C,X) for each class C in the
    // program. Here by "class" we mean SootClass, which could be a
    // Java class or a Java interface. The set of possible values of C
    // covers all SootClasses that are application classes or library
    // classes. X is a HashSet that contains SootClasses for all
    // elements of the set { C } union { D | D is a direct or
    // transitive subtype of C }, **excluding** all D that are
    // interfaces or abstract classes.
    private final Map<SootClass, Set<SootClass>> classAndItsConcreteSubTypes = Maps.newHashMap();

    // -------------------------------------------------------------
    // this hash table contains a pair (C,X) for each class C in the
    // program. Here by "class" we mean SootClass, which could be a
    // Java class or a Java interface. The set of possible values of C
    // covers all SootClasses that are application classes or library
    // classes. X is a HashSet that contains SootClasses for all
    // elements of the set { C } union { D | D is a direct or
    // transitive subtype of C }.
    private final Map<SootClass, Set<SootClass>> classAndItsSubTypes = Maps.newHashMap();

    // -------------------------------------------------------------
    // this hash table contains a pair (C,X) for each class C in the
    // program. Here by "class" we mean SootClass, which could be a
    // Java class or a Java interface. The set of possible values of C
    // covers all SootClasses that are application classes or library
    // classes. X is a HashSet that contains SootClasses for all
    // elements of the set { C } union { D | D is a direct or
    // transitive SUPERtype of C }.
    private final Map<SootClass, Set<SootClass>> classAndItsSuperTypes = Maps.newHashMap();

    public boolean isSubclassOf(
            final String childClassName,final String parentClassName) {
        SootClass child = Scene.v().getSootClass(childClassName);
        SootClass parent = Scene.v().getSootClass(parentClassName);
        return isSubclassOf(child, parent);
    }

    public boolean isSubclassOf(final SootClass child, final SootClass parent) {
        Set<SootClass> superTypes = getSupertypes(child);
        if (superTypes != null) {
            return superTypes.contains(parent);
        }
        return isSubclassOfOnDemand(child, parent);
    }

    public boolean isSubclassOfOnDemand(final SootClass child, final SootClass parent) {
        return parent.getName().equals("java.lang.Object") ||
                child.equals(parent) ||
                child.hasSuperclass() &&
                        isSubclassOfOnDemand(child.getSuperclass(), parent);
    }

    public boolean isActivityClass(final SootClass c) {
        return isSubclassOf(c, Scene.v().getSootClass("android.app.Activity"));
    }

    // -----------------------------------------
    // Returns a set of SootClasses: all transitive subtypes of c,
    // including c
    public Set<SootClass> getSubtypes(SootClass c) {
        return classAndItsSubTypes.get(c);
    }

    // -----------------------------------------
    // Returns a set of SootClasses: all transitive SUPERtypes of c,
    // including c
    public Set<SootClass> getSupertypes(SootClass c) {
        return classAndItsSuperTypes.get(c);
    }

    // ----------------------------------------------------------
    // Returns a set of SootClasses: all transitive subtypes of c
    // (including c) for which SootClass.isConcrete() is true
    public Set<SootClass> getConcreteSubtypes(SootClass c) {
        return classAndItsConcreteSubTypes.get(c);
    }


}
