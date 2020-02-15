package com.example.juan.tanit.selfadaptation;

import android.app.Activity;
import android.util.Log;

import java.util.List;

import com.carrotsearch.hppc.IntArrayList;
import com.example.juan.tanit.dspl.BasicFix;
import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.FeatureModel;
import com.example.juan.tanit.plan.DSPLPlanDescription;
import com.example.juan.tanit.plan.Plan;
import com.example.juan.tanit.plan.PlanDescription;
import com.example.juan.tanit.plan.SAPlanDescription;
import com.example.juan.tanit.plan.SelfAdaptationPlan;
import com.example.juan.tanit.selfadaptation.goal.Goal;

public class DSPLEngine extends SelfAdaptationEngine {

    private FeatureModel featureModel;
    private Configuration currentConfiguration=null;
    //private HashMap<String,SAComponentInterface> configurableComponents;

    public DSPLEngine(Activity ac) {
        super(ac);
        //configurableComponents=new HashMap<>();
        featureModel=new FeatureModel();
    }

    public DSPLEngine(Activity ac,FeatureModel fm){
        super(ac);
        featureModel=fm;
    }

    public DSPLEngine(Activity act, FeatureModel fm, Configuration c/*,HashMap<String,SAComponentInterface> cc*/){
        super(act);
        featureModel=fm;
        currentConfiguration=c;
        //configurableComponents=cc;
    }

    @Override
    protected Plan selectPlan(Goal goal) {
        Plan plan = null;
        List<PlanDescription> pdList = getPlanLibrary().getPlan(goal);

        // Selecciona un plan de la lista que no se ha ejecutado anteriormente.
        if (!pdList.isEmpty()) {
            Boolean success=false;
            int index=0;
            while (!success && index<pdList.size()){
                if((pdList.get(index) instanceof DSPLPlanDescription) || (pdList.get(index)instanceof SAPlanDescription)){
                    if(pdList.get(index).checkPreCondition(currentConfiguration) &&
                            !goal.tried(pdList.get(index).getPlanClass())){
                        plan = pdList.get(index).instantiatePlan();
                        if (plan instanceof SelfAdaptationPlan) {
                            SelfAdaptationPlan saPlan = (SelfAdaptationPlan) plan;
                            if (saPlan.checkPrecondition(currentConfiguration)) {
                                // Creamos una copia de la configuración actual del sistema.
                                Configuration quickConfiguration = getQuickConfiguration(saPlan);
                                // ¿Es correcta la configuración resultante?
                                if (quickConfiguration.checkTreeConstraints() && quickConfiguration.checkCrossTreeConstraints()) {
                                    setupPlan(saPlan, quickConfiguration);
                                    success = true;
                                } else {
                                    BasicFix basicFix=new BasicFix();
                                    Configuration fixedConfiguration =new Configuration(featureModel);
                                    if (basicFix.fix(quickConfiguration, fixedConfiguration)){//, toInclude, toExclude)) {
                                        setupPlan(saPlan, fixedConfiguration);//fixedConfiguration);
                                        success = true;
                                    } else {
                                        plan = null;
                                    }
                                }
                            }else{
                                // Si no se cumple la pre-condición damos el plan por nulo
                                plan=null;
                            }
                        }else{
                            // al ser un plan normal, lo único que hay que hacer es instanciarlo.
                            success=true;
                        }

                    }
                }else{
                    if(pdList.get(index).checkPreCondition(getContext()) &&
                            !goal.tried(pdList.get(index).getPlanClass())) {
                        plan = pdList.get(index).instantiatePlan();
                        success=true;
                    }
                }
                index++;
            }
        }
        return plan;
    }

    protected Configuration getQuickConfiguration(SelfAdaptationPlan saPlan){
        Configuration newConfiguration = currentConfiguration.getClone();
        // Añadimos las características que deben de añadir según nuestro plan.
        for (String newFeature : saPlan.getmIncluded()) {
            if (!newConfiguration.contains(featureModel.getID(newFeature))) {
                newConfiguration.add(featureModel.getID(newFeature));
            }
        }
        // eliminamos las características que deben de borrar según nuestro plan
        for (String removedFeature : saPlan.getmExcluded()) {
            if (newConfiguration.contains(featureModel.getID(removedFeature))) {
                newConfiguration.remove(featureModel.getID(removedFeature));
            }
        }
        return newConfiguration;
    }

    protected Configuration getMinimalConfiguration(SelfAdaptationPlan saPlan){
        Configuration newConfiguration = new Configuration(getFeatureModel());
        // Añadimos las características que deben de añadir según nuestro plan.
        for (String newFeature : saPlan.getmIncluded()) {
            if (!newConfiguration.contains(featureModel.getID(newFeature))) {
                newConfiguration.add(featureModel.getID(newFeature));
            }
        }
        return newConfiguration;
    }

    // se le indica al plan de auto-adaptación que características deben de añadirse o eliminarsse.
    protected void setupPlan(SelfAdaptationPlan plan,Configuration newConfiguration){


        Log.d("Self","Configuración inicial: "+currentConfiguration.toString());
        Log.d("Self","Configuración objetivo: "+newConfiguration.toString());
        plan.setNewConfiguration(newConfiguration);
        IntArrayList features=featureModel.getFeatures();
        IntArrayList currentConfigurationList=currentConfiguration.getConfigurationByID();
        IntArrayList targetConfigurationList=newConfiguration.getConfigurationByID();

        for(int i=0;i<features.size();i++){
            if(currentConfigurationList.contains(features.get(i))){
                if(targetConfigurationList.contains(features.get(i))){
                    // no se hace nada
                }else{
                    // se tiene que borrar
                    plan.addToRemoveFeature(featureModel.getName(features.get(i)));
                }
            }else{
                if(targetConfigurationList.contains(features.get(i))){
                    // se tiene que añadir a la configuración final.
                    plan.addToIncludeFeature(featureModel.getName(features.get(i)));
                }else{
                    // si no está en ninguno de los 2 no tenemos que hacer nada.
                }
            }
        }

        /*boolean[] currentConfigurationArray=currentConfiguration.getConfiguration();
        boolean[] newConfigurationArray=newConfiguration.getConfiguration();
        plan.setNewConfiguration(newConfiguration);
        for(int i=0;i<currentConfigurationArray.length;i++){
            // si los valores son distintos
            if(currentConfigurationArray[i]!=newConfigurationArray[i]){
                Log.d("Self","Diferencias entre "+featureModel.getName(i));
                if(currentConfigurationArray[i]){
                    // el plan de auto-adaptación tiene que eliminar la característica
                    plan.addToRemoveFeature(featureModel.getName(i));
                }else{
                    // el plan de auto-adaptación tiene que añadir la característica
                    plan.addToIncludeFeature(featureModel.getName(i));
                }
            }
        }*/
    }

    @Override
    public void planSucceed(Plan p) {
        super.planSucceed(p);
        // Si el plan es de auto-adaptación y se ha ejecutado con éxito hacemos una actualización de la configuración
        if(p instanceof SelfAdaptationPlan){
            Log.d("Self","Configuración antes de la adaptación "+currentConfiguration.toString());
            SelfAdaptationPlan saPlan=(SelfAdaptationPlan)p;
            //Configuration newConfiguration=saPlan.getNewConfiguration();
            currentConfiguration=saPlan.getNewConfiguration();
            /*if(applyConfiguration(newConfiguration)) {
                currentConfiguration = saPlan.getNewConfiguration();
            }*/
            Log.d("Self","Configuración después de la adaptación "+currentConfiguration.toString());
        }
    }


    /*protected Boolean applyConfiguration(Configuration newConfiguration){
        Boolean success=true;
        if(!newConfiguration.equals(currentConfiguration)){
            // se calculan las diferencias
            // diffConfiguration tiene habilitadas aquellas características que son diferentes
            // entre las 2 configuraciones.
            Configuration diffConfiguration=currentConfiguration.getDifferences(newConfiguration);
            IntArrayList diffConfigurationList=diffConfiguration.getConfigurationByID();
            int index=0;
            while(index<diffConfigurationList.size() && success) {
                // hay que habilitar o deshabilitar
                // si está en la configuración actual y aparece en diferencias hay que deshabilitar
                String compId = featureModel.getName(diffConfigurationList.get(index));
                if (currentConfiguration.contains(diffConfigurationList.get(index))) {
                    SAComponentInterface component = configurableComponents.get(compId);
                    component.disable();
                    // si no está en la configuración actual y aparece en diferencias hay que habilitar.
                } else {
                    SAComponentInterface component = configurableComponents.get(compId);
                    component.enable();
                }
            }
            index++;
        }
        return success;
    }*/

    public void setFeatureModel(FeatureModel fm){
        featureModel=fm;
    }

    public FeatureModel getFeatureModel() {
        return featureModel;
    }

    public Configuration getCurrentConfiguration() {
        if(currentConfiguration==null){
            currentConfiguration=new Configuration(featureModel);
        }
        return currentConfiguration;
    }

    public void setCurrentConfiguration(Configuration c){
        currentConfiguration=c;
    }
}
