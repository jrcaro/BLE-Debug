package com.example.juan.tanit.selfadaptation;

import android.app.Activity;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.juan.tanit.dspl.BasicFix;
import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.FeatureModel;
import com.example.juan.tanit.dspl.PreferenceBasedFeatureModel;
import com.example.juan.tanit.dspl.StringCrossTreeConstraint;
import com.example.juan.tanit.plan.DSPLPlanDescription;
import com.example.juan.tanit.plan.Plan;
import com.example.juan.tanit.plan.PlanDescription;
import com.example.juan.tanit.plan.PlanPreference;
import com.example.juan.tanit.plan.PreferenceBasedPlanLibrary;
import com.example.juan.tanit.plan.SAPlanDescription;
import com.example.juan.tanit.plan.SSAdaptationPlan;
import com.example.juan.tanit.plan.SelfAdaptationPlan;
import com.example.juan.tanit.selfadaptation.goal.Goal;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class PreferenceBasedEngine extends DSPLEngine {

    //private PreferenceBasedFeatureModel pbFeatureModel;
    private PreferenceUpdater preferenceUpdater;
    private Double maxWellness, maxUsefulness;
    private Double alfa;

    public PreferenceBasedEngine(Activity act){
        super(act);
        preferenceUpdater=new PreferenceUpdater(this);
        alfa=0.8;
        setPlanLibrary(new PreferenceBasedPlanLibrary());
        setFeatureModel(new PreferenceBasedFeatureModel());
        //pbFeatureModel=(PreferenceBasedFeatureModel) getFeatureModel();
    }

    public PreferenceBasedEngine(Activity act,PreferenceBasedFeatureModel fm){
        super(act,fm);
        preferenceUpdater=new PreferenceUpdater(this);
        alfa=0.8;
        setPlanLibrary(new PreferenceBasedPlanLibrary());
        setFeatureModel(fm);
        //pbFeatureModel=fm;
    }

    public PreferenceBasedEngine(Activity act, FeatureModel fm, Configuration c) {
        super(act,fm,c);
        //pbFeatureModel=(PreferenceBasedFeatureModel)fm;
        preferenceUpdater=new PreferenceUpdater(this);
        alfa=0.8;
        // la librería tiene que ser basada en preferencia
        setPlanLibrary(new PreferenceBasedPlanLibrary());
        setFeatureModel(fm);
    }

    public void addWellnessFactor(String id, Optimization m,Double crit_value){
        preferenceUpdater.addWellnessFactor(id,m,crit_value);
    }

    public void addWellnessFactor(WellnessFactor wf){
        preferenceUpdater.addWellnessFactor(wf);
    }

    public void updateWellnessFactor(String id,Double val){
        preferenceUpdater.updateWellnessFactor(id,val);
    }

    @Override
    protected Plan selectPlan(Goal goal) {
        // 1. Recuperamos todos los planes asociados a un objetivo.
        PreferenceBasedPlanLibrary library=(PreferenceBasedPlanLibrary)getPlanLibrary();
        List<PlanPreference> pwList=library.getPlanWellness(goal);
        Plan res=null;

        // Se computa la preferencia y se ordena para cada plan
        TreeMap<Double, PlanDescription> orderedPlanList=new TreeMap<>();
        for(int i=0;i<pwList.size();i++){
            if(!goal.tried(pwList.get(i).getPlanDescription().getPlanClass())) {
                if ((pwList.get(i).getPlanDescription() instanceof DSPLPlanDescription) ||
                        (pwList.get(i).getPlanDescription() instanceof SAPlanDescription)) {
                    if ((pwList.get(i).getPlanDescription().checkPreCondition(getCurrentConfiguration()))) {
                        Double preference = computePreference(pwList.get(i));
                        orderedPlanList.put(preference, pwList.get(i).getPlanDescription());
                    }
                } else {
                    if (pwList.get(i).getPlanDescription().checkPreCondition(getContext())) {
                        Double preference = computePreference(pwList.get(i));
                        orderedPlanList.put(preference, pwList.get(i).getPlanDescription());
                    }
                }
            }
        }
        Map.Entry<Double,PlanDescription> lastE=orderedPlanList.pollLastEntry();
        Boolean success=false;
        while(lastE!=null && !success){
            res=lastE.getValue().instantiatePlan();
            Log.d("baking","¿Aplicamos "+res.getClass().toString()+"?");
            if((res instanceof SelfAdaptationPlan) || (res instanceof SSAdaptationPlan)){
                //Log.d("baking","El plan es SelfAdaptation o SSAdapatation");
                SelfAdaptationPlan saPlan=(SelfAdaptationPlan)res;
                Configuration quickConfiguration = getQuickConfiguration(saPlan);
                if (quickConfiguration.checkTreeConstraints() && quickConfiguration.checkCrossTreeConstraints()) {
                    Log.d("baking","El plan pasa la condición A y se ejecuta.");
                    setupPlan(saPlan, quickConfiguration);
                    success=true;
                }else{
                    //Log.d("baking","B");
                    BasicFix basicFix=new BasicFix();
                    Configuration minimalConfiguration =getMinimalConfiguration(saPlan);
                    Configuration emptyConfiguration=new Configuration(getFeatureModel());
                    Log.d("baking","La configuración a arreglar es "+minimalConfiguration.toString());
                    int attempt=0;
                    success=basicFix.fix(minimalConfiguration, emptyConfiguration);
                    while (!success && attempt<1000){//, toInclude, toExclude)) {
                        //setupPlan(saPlan, emptyConfiguration);//minimalConfiguration);
                        success = basicFix.fix(minimalConfiguration, emptyConfiguration);
                        //Log.d("baking","C");
                        attempt++;
                    }
                    if(success){
                        Log.d("baking","la configuración ha podido arreglarse");
                        Log.d("baking","La configuración arreglada es "+emptyConfiguration.toString());
                        setupPlan(saPlan,emptyConfiguration);
                    }else{
                        Log.d("baking","No ha sido posible arreglar la configuración...");
                    }
                }
            }
            lastE=orderedPlanList.pollLastEntry();
        }
        if(success){
            Log.d("baking","El plan seleccionado para la ejecución es "+res.getClass().toString());
        }else{
            Log.d("baking"," No se ha seleccionado ningún plan.");
            res=null;
        }
        return res;
    }

    @Override
    public void run() {
        // se calcula la preferencia y la usefullness máxima.
        computeMaxUsefulness();
        computeMaxWellness();
        preferenceUpdater.start();
        super.run();
    }

    // con la implementación actual sería 3 -> porcentaje de objetivos activos
    // + porcentaje de servicios al 100% + máxima calidad de un plan.
    private void computeMaxUsefulness(){
        maxUsefulness =3.0;
    }

    // todos los factores de wellness a su máximo valor.
    private void computeMaxWellness(){

        maxWellness=new Double(preferenceUpdater.getNumberOfWellnessFactor());
    }

    private Double computePreference(PlanPreference pp) {
        Double res = 0.0;
        Double usefulness;
        // se computa el efecto de la ejecución en la wellness del agente.
        Double computedPlanEffect = pp.computePlanEffect(preferenceUpdater.getCurrentWellnessFactor());


        // se computa usefulness
        // si no es un plan de auto-adaptación se devuelve la usefulness actual del sistema
        Plan plan = pp.getPlanDescription().instantiatePlan();
        Double architecturalUsefulness=0.0;
        if (plan instanceof SelfAdaptationPlan) {
            SelfAdaptationPlan saPlan = (SelfAdaptationPlan) plan;
            // Obtenemos una configuración rápida y calculamos su preferencia.
            Configuration newConfiguration = getQuickConfiguration(saPlan);
            if (newConfiguration.checkTreeConstraints() && newConfiguration.checkCrossTreeConstraints()) {
                // cuál es la usefulness de esta configuración?
                architecturalUsefulness=preferenceUpdater.computeUsefulness(newConfiguration);
            } else {
                BasicFix basicFix = new BasicFix();
                Configuration fixedConfiguration = new Configuration(getFeatureModel());
                if (basicFix.fix(newConfiguration, fixedConfiguration)) {//, toInclude, toExclude)) {
                    architecturalUsefulness=preferenceUpdater.computeUsefulness(newConfiguration);
                } else {
                    // no ha sido posible arreglar la configuración -> el plan no debería de aplicarse;
                    architecturalUsefulness=-1.0;
                }

            }
        }
        if(architecturalUsefulness.intValue()==-1){
            usefulness=-1.0;
        }else{
            usefulness=architecturalUsefulness+pp.getPlanQuality();
        }
        // aplicamos la expresión del MATES.
        Double a1=usefulness/maxUsefulness;
        Double a21=(computedPlanEffect/maxWellness)*(-1);
        Double a22=Math.exp(a21.doubleValue());
        Double doubleRes=a1 - alfa*a22;
        res=doubleRes;
        return res;
    }

    public void registerPlanDescription(GoalDescription gd, PlanDescription pd, PlanPreference pp) {
        super.registerPlanDescription(gd, pd);
        ((PreferenceBasedFeatureModel)getFeatureModel()).registerPlan(gd, pd);
        PreferenceBasedPlanLibrary preferenceBasedPlanLibrary=(PreferenceBasedPlanLibrary)getPlanLibrary();
        preferenceBasedPlanLibrary.registerPlanWellness(gd,pp);
    }

    public void registerPlanDescription(GoalDescription gd, PlanDescription pd){
        super.registerPlanDescription(gd, pd);
        ((PreferenceBasedFeatureModel)getFeatureModel()).registerPlan(gd, pd);
    }

    public void registerPlanWellness(GoalDescription gd,PlanPreference pp){
        PreferenceBasedPlanLibrary preferenceBasedPlanLibrary=(PreferenceBasedPlanLibrary)getPlanLibrary();
        preferenceBasedPlanLibrary.registerPlanWellness(gd,pp);
    }

    public void registerService(String id, HashMap<String, StringCrossTreeConstraint> qualityValues,
                                HashMap<String,List<String>> parameterTable, Boolean mandatory){
        ((PreferenceBasedFeatureModel)getFeatureModel()).registerService(id, qualityValues, parameterTable,mandatory);
    }
}
