package dulinglai.android.alode.graphBuilder;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.solver.IDESolver;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGPostDominatorsFinder;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ICFG implements IInfoflowCFG{

    private final static int MAX_SIDE_EFFECT_ANALYSIS_DEPTH = 25;
    private final static int MAX_STATIC_USE_ANALYSIS_DEPTH = 50;

    private enum StaticFieldUse {
        Unknown, Unused, Read, Write, ReadWrite
    }

    protected final Map<SootMethod, Map<SootField, StaticFieldUse>> staticFieldUses = new ConcurrentHashMap<>();
    protected final Map<SootMethod, Boolean> methodSideEffects = new ConcurrentHashMap<>();

    protected final BiDiInterproceduralCFG<Unit, SootMethod> delegate;

    protected final LoadingCache<Unit, UnitContainer> unitToPostdominator = IDESolver.DEFAULT_CACHE_BUILDER
            .build(new CacheLoader<Unit, UnitContainer>() {
                @Override
                public UnitContainer load(Unit unit) {
                    SootMethod method = getMethodOf(unit);
                    DirectedGraph<Unit> graph = delegate.getOrCreateUnitGraph(method);

                    MHGPostDominatorsFinder<Unit> postdominatorFinder = new MHGPostDominatorsFinder<Unit>(graph);
                    Unit postdom = postdominatorFinder.getImmediateDominator(unit);
                    if (postdom == null)
                        return new UnitContainer(method);
                    else
                        return new UnitContainer(postdom);
                }
            });

    protected final LoadingCache<SootMethod, Local[]> methodToUsedLocals = IDESolver.DEFAULT_CACHE_BUILDER
            .build(new CacheLoader<SootMethod, Local[]>() {
                @Override
                public Local[] load(SootMethod method) {
                    if (!method.isConcrete() || !method.hasActiveBody())
                        return new Local[0];

                    List<Local> lcs = new ArrayList<Local>(method.getParameterCount() + (method.isStatic() ? 0 : 1));

                    for (Unit u : method.getActiveBody().getUnits())
                        useBox: for (ValueBox vb : u.getUseBoxes()) {
                            // Check for parameters
                            for (int i = 0; i < method.getParameterCount(); i++) {
                                if (method.getActiveBody().getParameterLocal(i) == vb.getValue()) {
                                    lcs.add((Local) vb.getValue());
                                    continue useBox;
                                }
                            }
                        }

                    // Add the "this" local
                    if (!method.isStatic())
                        lcs.add(method.getActiveBody().getThisLocal());

                    return lcs.toArray(new Local[lcs.size()]);
                }
            });

    protected final LoadingCache<SootMethod, Local[]> methodToWrittenLocals = IDESolver.DEFAULT_CACHE_BUILDER
            .build(new CacheLoader<SootMethod, Local[]>() {
                @Override
                public Local[] load(SootMethod method) {
                    if (!method.isConcrete() || !method.hasActiveBody())
                        return new Local[0];

                    List<Local> lcs = new ArrayList<Local>(method.getActiveBody().getLocalCount());

                    for (Unit u : method.getActiveBody().getUnits())
                        if (u instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) u;
                            if (assignStmt.getLeftOp() instanceof Local)
                                lcs.add((Local) assignStmt.getLeftOp());
                        }

                    return lcs.toArray(new Local[lcs.size()]);
                }
            });

    public ICFG(){
        this(new JimpleBasedInterproceduralCFG(true, true));
    }

    public ICFG(BiDiInterproceduralCFG<Unit,SootMethod> delegate){
        this.delegate = delegate;
    }

    @Override
    public UnitContainer getPostdominatorOf(Unit u) {
        return unitToPostdominator.getUnchecked(u);
    }

    // delegate methods follow

    @Override
    public SootMethod getMethodOf(Unit u) {
        return delegate.getMethodOf(u);
    }

    @Override
    public List<Unit> getSuccsOf(Unit u) {
        return delegate.getSuccsOf(u);
    }

    @Override
    public boolean isExitStmt(Unit u) {
        return delegate.isExitStmt(u);
    }

    @Override
    public boolean isStartPoint(Unit u) {
        return delegate.isStartPoint(u);
    }

    @Override
    public boolean isFallThroughSuccessor(Unit u, Unit succ) {
        return delegate.isFallThroughSuccessor(u, succ);
    }

    @Override
    public boolean isBranchTarget(Unit u, Unit succ) {
        return delegate.isBranchTarget(u, succ);
    }

    @Override
    public Collection<Unit> getStartPointsOf(SootMethod m) {
        return delegate.getStartPointsOf(m);
    }

    @Override
    public boolean isCallStmt(Unit u) {
        return delegate.isCallStmt(u);
    }

    @Override
    public Set<Unit> allNonCallStartNodes() {
        return delegate.allNonCallStartNodes();
    }

    @Override
    public Collection<SootMethod> getCalleesOfCallAt(Unit u) {
        return delegate.getCalleesOfCallAt(u);
    }

    @Override
    public Collection<Unit> getCallersOf(SootMethod m) {
        return delegate.getCallersOf(m);
    }

    @Override
    public Collection<Unit> getReturnSitesOfCallAt(Unit u) {
        return delegate.getReturnSitesOfCallAt(u);
    }

    @Override
    public Set<Unit> getCallsFromWithin(SootMethod m) {
        return delegate.getCallsFromWithin(m);
    }

    @Override
    public List<Unit> getPredsOf(Unit u) {
        return delegate.getPredsOf(u);
    }

    @Override
    public Collection<Unit> getEndPointsOf(SootMethod m) {
        return delegate.getEndPointsOf(m);
    }

    @Override
    public List<Unit> getPredsOfCallAt(Unit u) {
        return delegate.getPredsOf(u);
    }

    @Override
    public Set<Unit> allNonCallEndNodes() {
        return delegate.allNonCallEndNodes();
    }

    @Override
    public DirectedGraph<Unit> getOrCreateUnitGraph(SootMethod m) {
        return delegate.getOrCreateUnitGraph(m);
    }

    @Override
    public List<Value> getParameterRefs(SootMethod m) {
        return delegate.getParameterRefs(m);
    }

    @Override
    public boolean isReturnSite(Unit n) {
        return delegate.isReturnSite(n);
    }

    @Override
    public boolean isStaticFieldRead(SootMethod method, SootField variable) {
        StaticFieldUse use = checkStaticFieldUsed(method, variable);
        return use == StaticFieldUse.Read || use == StaticFieldUse.ReadWrite || use == StaticFieldUse.Unknown;
    }

    @Override
    public boolean isStaticFieldUsed(SootMethod method, SootField variable) {
        StaticFieldUse use = checkStaticFieldUsed(method, variable);
        return use == StaticFieldUse.Write || use == StaticFieldUse.ReadWrite || use == StaticFieldUse.Unknown;
    }

    private synchronized StaticFieldUse checkStaticFieldUsed(SootMethod smethod, SootField variable) {
        // Skip over phantom methods
        if (!smethod.isConcrete())
            return StaticFieldUse.Unused;

        List<SootMethod> workList = new ArrayList<>();
        workList.add(smethod);
        MultiMap<SootMethod, SootMethod> methodToCallees = new HashMultiMap<>();
        Map<SootMethod, StaticFieldUse> tempUses = new HashMap<>();

        int processedMethods = 0;
        while (!workList.isEmpty()) {
            // DFS: We need to be able post-process a method once we know what all the
            // invocations do
            SootMethod method = workList.remove(workList.size() - 1);
            processedMethods++;

            // Without a body, we cannot say much
            if (!method.hasActiveBody())
                continue;

            // Limit the maximum analysis depth
            if (processedMethods > MAX_STATIC_USE_ANALYSIS_DEPTH)
                return StaticFieldUse.Unknown;

            boolean hasInvocation = false;
            boolean reads = false, writes = false;

            // Do we already have a cache entry?
            Map<SootField, StaticFieldUse> entry = staticFieldUses.get(method);
            if (entry != null) {
                StaticFieldUse b = entry.get(variable);
                if (b != null && b != StaticFieldUse.Unknown) {
                    tempUses.put(method, b);
                    continue;
                }
            }

            // Do we already have an entry?
            StaticFieldUse oldUse = tempUses.get(method);

            // Scan for references to this variable
            for (Unit u : method.getActiveBody().getUnits()) {
                if (u instanceof AssignStmt) {
                    AssignStmt assign = (AssignStmt) u;

                    if (assign.getLeftOp() instanceof StaticFieldRef) {
                        SootField sf = ((StaticFieldRef) assign.getLeftOp()).getField();
                        registerStaticVariableUse(method, sf, StaticFieldUse.Write);
                        if (variable.equals(sf))
                            writes = true;
                    }

                    if (assign.getRightOp() instanceof StaticFieldRef) {
                        SootField sf = ((StaticFieldRef) assign.getRightOp()).getField();
                        registerStaticVariableUse(method, sf, StaticFieldUse.Read);
                        if (variable.equals(sf))
                            reads = true;
                    }
                }

                if (((Stmt) u).containsInvokeExpr())
                    for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(u); edgeIt.hasNext();) {
                        Edge e = edgeIt.next();
                        SootMethod callee = e.getTgt().method();
                        if (callee.isConcrete()) {
                            // Do we already know this method?
                            StaticFieldUse calleeUse = tempUses.get(callee);
                            if (calleeUse == null) {
                                // We need to get back to the current method after we have processed the callees
                                if (!hasInvocation)
                                    workList.add(method);

                                // Process the callee
                                workList.add(callee);
                                methodToCallees.put(method, callee);
                                hasInvocation = true;
                            } else {
                                reads |= calleeUse == StaticFieldUse.Read || calleeUse == StaticFieldUse.ReadWrite;
                                writes |= calleeUse == StaticFieldUse.Write || calleeUse == StaticFieldUse.ReadWrite;
                            }
                        }
                    }
            }

            // Variable is not read
            StaticFieldUse fieldUse = StaticFieldUse.Unused;
            if (reads && writes)
                fieldUse = StaticFieldUse.ReadWrite;
            else if (reads)
                fieldUse = StaticFieldUse.Read;
            else if (writes)
                fieldUse = StaticFieldUse.Write;

            // Have we changed our previous state?
            if (fieldUse == oldUse)
                continue;
            tempUses.put(method, fieldUse);
        }

        // Merge the temporary results into the global cache
        for (Map.Entry<SootMethod, StaticFieldUse> tempEntry : tempUses.entrySet()) {
            registerStaticVariableUse(tempEntry.getKey(), variable, tempEntry.getValue());
        }

        StaticFieldUse outerUse = tempUses.get(smethod);
        return outerUse == null ? StaticFieldUse.Unknown : outerUse;
    }

    private void registerStaticVariableUse(SootMethod method, SootField variable, StaticFieldUse fieldUse) {
        Map<SootField, StaticFieldUse> entry = staticFieldUses.get(method);
        StaticFieldUse oldUse;
        synchronized (staticFieldUses) {
            if (entry == null) {
                entry = new ConcurrentHashMap<SootField, StaticFieldUse>();
                staticFieldUses.put(method, entry);
                entry.put(variable, fieldUse);
                return;
            }

            oldUse = entry.get(variable);
            if (oldUse == null) {
                entry.put(variable, fieldUse);
                return;
            }
        }

        // This part is monotonic, so no need for synchronization
        StaticFieldUse newUse;
        switch (oldUse) {
            case Unknown:
            case Unused:
            case ReadWrite:
                newUse = fieldUse;
                break;
            case Read:
                newUse = (fieldUse == StaticFieldUse.Read) ? oldUse : StaticFieldUse.ReadWrite;
                break;
            case Write:
                newUse = (fieldUse == StaticFieldUse.Write) ? oldUse : StaticFieldUse.ReadWrite;
                break;
            default:
                throw new RuntimeException("Invalid field use");
        }
        entry.put(variable, newUse);
    }

    @Override
    public boolean hasSideEffects(SootMethod method) {
        return hasSideEffects(method, new HashSet<SootMethod>(), 0);
    }

    private boolean hasSideEffects(SootMethod method, Set<SootMethod> runList, int depth) {
        // Without a body, we cannot say much
        if (!method.hasActiveBody())
            return false;

        // Do not process the same method twice
        if (!runList.add(method))
            return false;

        // Do we already have an entry?
        Boolean hasSideEffects = methodSideEffects.get(method);
        if (hasSideEffects != null)
            return hasSideEffects;

        // Limit the maximum analysis depth
        if (depth > MAX_SIDE_EFFECT_ANALYSIS_DEPTH)
            return true;

        // Scan for references to this variable
        for (Unit u : method.getActiveBody().getUnits()) {
            if (u instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) u;

                if (assign.getLeftOp() instanceof FieldRef) {
                    methodSideEffects.put(method, true);
                    return true;
                }
            }

            if (((Stmt) u).containsInvokeExpr())
                for (Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(u); edgeIt.hasNext();) {
                    Edge e = edgeIt.next();
                    if (hasSideEffects(e.getTgt().method(), runList, depth++))
                        return true;
                }
        }

        // Variable is not read
        methodSideEffects.put(method, false);
        return false;
    }

    @Override
    public void notifyMethodChanged(SootMethod m) {
        if (delegate instanceof JimpleBasedInterproceduralCFG)
            ((JimpleBasedInterproceduralCFG) delegate).initializeUnitToOwner(m);
    }

    @Override
    public boolean methodReadsValue(SootMethod m, Value v) {
        Local[] reads = methodToUsedLocals.getUnchecked(m);
        if (reads != null)
            for (Local l : reads)
                if (l == v)
                    return true;
        return false;
    }

    @Override
    public boolean methodWritesValue(SootMethod m, Value v) {
        Local[] writes = methodToWrittenLocals.getUnchecked(m);
        if (writes != null)
            for (Local l : writes)
                if (l == v)
                    return true;
        return false;
    }

    @Override
    public boolean isExceptionalEdgeBetween(Unit u1, Unit u2) {
        SootMethod m1 = getMethodOf(u1);
        SootMethod m2 = getMethodOf(u2);
        if (m1 != m2)
            throw new RuntimeException("Exceptional edges are only supported " + "inside the same method");
        DirectedGraph<Unit> ug1 = getOrCreateUnitGraph(m1);

        // Exception tracking might be disabled
        if (!(ug1 instanceof ExceptionalUnitGraph))
            return false;

        ExceptionalUnitGraph eug = (ExceptionalUnitGraph) ug1;
        return eug.getExceptionalSuccsOf(u1).contains(u2);
    }

    @Override
    public boolean isReachable(Unit u) {
        return delegate.isReachable(u);
    }

    @Override
    public boolean isExecutorExecute(InvokeExpr ie, SootMethod dest) {
        if (ie == null || dest == null)
            return false;

        SootMethod ieMethod = ie.getMethod();
        if (!ieMethod.getName().equals("execute") && !ieMethod.getName().equals("doPrivileged"))
            return false;

        final String ieSubSig = ieMethod.getSubSignature();
        final String calleeSubSig = dest.getSubSignature();

        if (ieSubSig.equals("void execute(java.lang.Runnable)") && calleeSubSig.equals("void run()"))
            return true;

        if (calleeSubSig.equals("java.lang.Object run()")) {
            if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedAction)"))
                return true;
            if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedAction,"
                    + "java.security.AccessControlContext)"))
                return true;
            if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)"))
                return true;
            return ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,"
                    + "java.security.AccessControlContext)");
        }
        return false;
    }

    @Override
    public Collection<SootMethod> getOrdinaryCalleesOfCallAt(Unit u) {
        InvokeExpr iexpr = ((Stmt) u).getInvokeExpr();

        Collection<SootMethod> originalCallees = getCalleesOfCallAt(u);
        List<SootMethod> callees = new ArrayList<>(originalCallees.size());
        for (SootMethod sm : originalCallees)
            if (!sm.isStaticInitializer() && !isExecutorExecute(iexpr, sm))
                callees.add(sm);
        return callees;
    }

    @Override
    public boolean isReflectiveCallSite(Unit u) {
        if (isCallStmt(u)) {
            InvokeExpr iexpr = ((Stmt) u).getInvokeExpr();
            return isReflectiveCallSite(iexpr);
        }
        return false;
    }

    @Override
    public boolean isReflectiveCallSite(InvokeExpr iexpr) {
        if (iexpr instanceof VirtualInvokeExpr) {
            VirtualInvokeExpr viexpr = (VirtualInvokeExpr) iexpr;
            if (viexpr.getBase().getType() instanceof RefType)
                if (((RefType) viexpr.getBase().getType()).getSootClass().getName().equals("java.lang.reflect.Method"))
                    return viexpr.getMethod().getName().equals("invoke");
        }
        return false;
    }

    @Override
    public void purge() {
        methodSideEffects.clear();
        staticFieldUses.clear();

        methodToUsedLocals.invalidateAll();
        methodToUsedLocals.cleanUp();

        methodToWrittenLocals.invalidateAll();
        methodToWrittenLocals.cleanUp();

        unitToPostdominator.invalidateAll();
        unitToPostdominator.cleanUp();
    }

}
