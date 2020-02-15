package com.example.juan.tanit.selfadaptation;

import android.app.Activity;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.example.juan.tanit.plan.Plan;
import com.example.juan.tanit.plan.PlanDescription;
import com.example.juan.tanit.plan.PlanExecutor;
import com.example.juan.tanit.plan.PlanLibrary;
import com.example.juan.tanit.selfadaptation.goal.Goal;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class SelfAdaptationEngine extends Thread {

    private Boolean end;
    private Integer waitingTime;
    private PlanExecutor planExecutor;
    private Context context;
    private PlanLibrary planLibrary;



    // locks
    private final ReadWriteLock purReadWriteLock = new ReentrantReadWriteLock();
    private final Lock purReadLock = purReadWriteLock.readLock();
    private final Lock purWriteLock = purReadWriteLock.writeLock();

    private final ReadWriteLock procReadWriteLock = new ReentrantReadWriteLock();
    private final Lock procReadLock = procReadWriteLock.readLock();
    private final Lock procWriteLock = procReadWriteLock.writeLock();

    // Goals
    private List<GoalDescription> availableGoals;
    private List<Goal> pursuing, processed;


    public SelfAdaptationEngine(Activity ac) {
        end = false;
        waitingTime = 100;
        planExecutor = new PlanExecutor(ac,this);
        availableGoals = new LinkedList<>();
        pursuing = new LinkedList<>();
        processed = new LinkedList<>();
        context = new Context();
        planLibrary = new PlanLibrary();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        planExecutor.start();
        while (!end) {
            //Log.d("Self","Comienza iteración...");
            // Comprobamos si hay objetivos en la cola de procesados que eliminar.
            updateProcessedGoals();
            // comprobar si hay objetivos nuevos que instanciar
            List<Goal> goalToPursueList = reasonAboutGoals(context);
            //Log.d("Self","Se activan "+goalToPursueList.size()+" objetivos.");
            for (Goal goal : goalToPursueList) {
                pursueGoal(goal);
            }
            // Si hay objetivos que conseguir, cogemos un objetivo para ejecución
            if (pendingGoals()) {
                // obtenemos el objetivo
                Goal goalToPursue = getGoal();
                Log.d("Self","Pursuing "+goalToPursue.getClass().toString());
                // seleccionamos un plan para ejecutar este objetivo
                Plan plan = selectPlan(goalToPursue);
                // Se manda el plan para su ejecución
                if (plan!=null) {
                    Log.d("Self","Se ejecuta un plan para "+goalToPursue.getClass().toString());
                    planExecutor.addPlan(plan);
                    goalToPursue.registerAttempt(plan);
                    //metemos el objetivo en la lista de procesados
                    processGoal(goalToPursue);
                } else {
                    Log.d("Self","No hay planes para llevar a cabo este objetivo");
                    // este objetivo no puede llevarse a cabo con los planes que tenemos en nuestra librería.
                    // por ahora no hacemos nada con él.
                }
            }
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateProcessedGoals(){
        procWriteLock.lock();
        for (Goal processedGoal : processed) {
            processed.remove(processedGoal);
            if (!processedGoal.accomplished()) {
                pursueGoal(processedGoal);
            }
        }
        procWriteLock.unlock();
    }

    private void removeProcessedGoal(Goal g){
        procWriteLock.lock();
        processed.remove(g);
        procWriteLock.unlock();
    }

    public void cancelService() {
        planExecutor.cancelService();
        end = true;
    }

    // selecciona un plan que no ha sido ejecutado previamente para llevar a cabo este objetivo.
    protected Plan selectPlan(Goal goal) {
        Plan plan = null;
        List<PlanDescription> pdList = planLibrary.getPlan(goal);

        // Selecciona un plan de la lista que no se ha ejecutado anteriormente.
        if (!pdList.isEmpty()) {
            Boolean success=false;
            int index=0;
            while (!success && index<pdList.size() && pdList.get(index).checkPreCondition(context)){
                if(!goal.tried(pdList.get(index).getPlanClass())){
                    success=true;
                    plan=(Plan)pdList.get(index).instantiatePlan();
                }
                index++;
            }
        }
        return plan;
    }

    // instancia el objetivo
    private Goal instantiateGoal(GoalDescription gd) {
        Goal goal = null;
        try {
            // lo correcto hubiera sido tener un constructor con argumento la SelfAdaptationEngine
            goal = (Goal) gd.getGoalClass().newInstance();
            goal.setEngine(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return goal;
    }

    private void pursueGoal(Goal g) {
        purWriteLock.lock();
        pursuing.add(g);
        purWriteLock.unlock();
    }

    private Boolean isPursued(GoalDescription gd){
        Boolean res=false;
        int index=0;
        purReadLock.lock();
        while(index<pursuing.size() && !res){
            if(pursuing.get(index).getClass().equals( gd.getGoalClass())){
                res=true;
            }
            index++;
        }
        purReadLock.unlock();
        return res;
    }

    private Boolean isPursued(Goal g){
        Boolean res;
        purReadLock.lock();
        res=pursuing.contains(g);
        purReadLock.unlock();
        return res;
    }

    // Plan executor notifica a SAEngine que ha fallado la ejecución de un plan.
    // El protocolo en estos casos es volver a poner los objetivos procesados asociados a este plan (último plan que se ha ejecutado)
    // en la cola de pursuing de nuevo (a no ser de que está ya en esa cola, que no debería de ser posible).
    // Tenemos objetivos unitarios, sólo existe una instancia de un objetivo en todo el sistema, esto incluye la lista processed y pursuing
    public void planFailed(Plan p){
        List<Goal> attachedGoalList=new LinkedList<>();
        Log.d("Self","El plan "+p.getClass().toString()+" ha fallado.");
        procReadLock.lock();
        for(Goal procGoal:processed){
            List<Plan> attempts=procGoal.getAttempts();
            // Si p es el último plan que ha sido ejecutado para llevar a cabo este objetivo.
            if(attempts.get(attempts.size()-1).equals(p)){
                attachedGoalList.add(procGoal);
            }
        }
        procReadLock.unlock();
        // se borran esos objetivos de la lista de processed y se añaden de nuevo a lista de pursuing.
        for(Goal attachedGoal:attachedGoalList){
            removeProcessedGoal(attachedGoal);
            if(!isPursued(attachedGoal)){
                pursueGoal(attachedGoal);
            }
        }
    }

    public void planSucceed(Plan p){
        List<Goal> attachedGoalList=new LinkedList<>();
        procReadLock.lock();
        for(Goal procGoal:processed){
            List<Plan> attempts=procGoal.getAttempts();
            // Si p es el último plan que ha sido ejecutado para llevar a cabo este objetivo.
            if(attempts.get(attempts.size()-1).equals(p)){
                attachedGoalList.add(procGoal);
            }
        }
        procReadLock.unlock();
        // se borran esos objetivos de la lista de processed y se añaden de nuevo a lista de pursuing.
        for(Goal attachedGoal:attachedGoalList){
            removeProcessedGoal(attachedGoal);
        }
    }

    private void processGoal(Goal g){
        procWriteLock.lock();
        processed.add(g);
        procWriteLock.unlock();
    }



    private Boolean isProcessed(GoalDescription gd){
        Boolean res=false;
        int index=0;
        procReadLock.lock();
        while (index < processed.size() && !res){
            if(processed.get(index).getClass().equals(gd.getGoalClass())){
                res=true;
            }
            index++;
        }
        procReadLock.unlock();
        return res;
    }

    private Boolean pendingGoals() {
        purReadLock.lock();
        Boolean res = !pursuing.isEmpty();
        purReadLock.unlock();
        return res;
    }

    // se coge un objetivo de la lista de pursuing y se elimina de la lista.
    private Goal getGoal() {
        Goal goal = null;
        purWriteLock.lock();
        if (!pursuing.isEmpty()) {
            goal = pursuing.get(0);
            pursuing.remove(goal);
        }
        purWriteLock.unlock();
        return goal;
    }

    //
    public void handleEvent(Object input) {
        Log.d("Self","handleEvent");
        List<Goal> goalList = reasonAboutGoals(input);
        for (Goal goal : goalList) {
            pursueGoal(goal);
        }
    }

    public void registerPlanDescription(GoalDescription gd, PlanDescription pd) {
        planLibrary.registerPlanDescription(gd, pd);
        // comprobamos que el objetivo no ha sido ya registrado en nuestra librería.
        if (!availableGoals.contains(gd)) {
            availableGoals.add(gd);
        }
    }

    // Se instancian los objetivos que son activados por el contexto y se están buscando de manera activa, ni tampoco están siendo procesados.
    private List<Goal> reasonAboutGoals(Object input) {
        List<Goal> res = new LinkedList<>();
        for (GoalDescription gd : availableGoals) {
            if (gd.activate(input) && !isPursued(gd) && !isProcessed(gd)) {
                Goal goal = instantiateGoal(gd);
                res.add(goal);

            }
        }
        return res;
    }

    public PlanLibrary getPlanLibrary() {
        return planLibrary;
    }

    public void setPlanLibrary(PlanLibrary planLibrary) {
        this.planLibrary = planLibrary;
    }

    public void updateContext(String id,Object value){
        context.putValue(id,value);
    }

    public Object getContext(String id){
        return context.getValue(id);

    }
}
