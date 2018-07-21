package dulinglai.android.alode.config.soot;

import dulinglai.android.alode.config.GlobalConfigs;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.util.LinkedList;
import java.util.List;

class SootHelper {
    //
    void setSootExcludeLibs(Options options){
        // explicitly include packages for shorter runtime:
        List<String> excludeList = new LinkedList<String>();
        excludeList.add("java.*");
        excludeList.add("sun.*");
        excludeList.add("android.*");
        excludeList.add("org.apache.*");
        excludeList.add("org.eclipse.*");
        excludeList.add("soot.*");
        excludeList.add("javax.*");
        options.set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);
    }

    void setSootCallgraphAlgorithm (GlobalConfigs.CallgraphAlgorithm cg_algo) {
        switch (cg_algo) {
            case AutomaticSelection:
            case SPARK:
                Options.v().setPhaseOption("cg.spark", "on");
                break;
            case GEOM:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "geom-pta:true");
                // Those are default options, not sure whether removing them works.
                Options.v().setPhaseOption("cg.spark", "geom-encoding:Geom");
                Options.v().setPhaseOption("cg.spark", "geom-worklist:PQ");
                break;
            case CHA:
                Options.v().setPhaseOption("cg.cha", "on");
                break;
            case RTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "rta:true");
                Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
                break;
            case VTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "vta:true");
                break;
            default:
                throw new RuntimeException("Invalid callgraph algorithm");
        }
    }

    void loadSootClasses(){
        Scene.v().addBasicClass("android.R$id", SootClass.SIGNATURES);
        Scene.v().addBasicClass("com.android.internal.R$id", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.R$layout", SootClass.SIGNATURES);
        Scene.v().addBasicClass("com.android.internal.R$layout", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.R$menu", SootClass.SIGNATURES);
        Scene.v().addBasicClass("com.android.internal.R$menu", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.R$string", SootClass.SIGNATURES);
        Scene.v().addBasicClass("com.android.internal.R$string", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.app.Activity", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.app.ListActivity", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.widget.TabHost", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.widget.TabHost$TabSpec", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.widget.TabHost$TabContentFactory", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.view.LayoutInflater", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.view.View", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.DialogInterface$OnCancelListener", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.DialogInterface$OnKeyListener", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.content.DialogInterface$OnShowListener", SootClass.SIGNATURES);
        Scene.v().addBasicClass("android.app.AlertDialog", SootClass.SIGNATURES);
    }
}
