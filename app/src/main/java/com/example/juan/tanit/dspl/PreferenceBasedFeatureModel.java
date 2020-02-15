package com.example.juan.tanit.dspl;

import android.util.Log;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectOpenHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.example.juan.tanit.plan.DSPLPlanDescription;
import com.example.juan.tanit.plan.PlanDescription;
import com.example.juan.tanit.plan.SAPlanDescription;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class PreferenceBasedFeatureModel extends FeatureModel {

    private int rootId,sAEngineId,plansId,goalsId,servicesId;
    private IntObjectOpenHashMap<CrossTreeConstraint> goalCrossTreeConstraints;


    public PreferenceBasedFeatureModel() {
        super();
        // damos una estructura especial a este feature model.
        rootId=addFeature("system",0,true);
        sAEngineId=addFeature("saengine",rootId,true);
        plansId=addFeature("plans",sAEngineId,true);
        goalsId=addFeature("goals",sAEngineId,true);
        servicesId=addFeature("services",rootId,true);
        goalCrossTreeConstraints=new IntObjectOpenHashMap<>();
    }

    public void registerPlan(GoalDescription gd, PlanDescription pd){
        int thisPlanId, thisGoalId;
        CrossTreeConstraint crossTreeConstraint;
        IntArrayList positive,negative;

        // Se añade el plan a la arquitectura
        thisPlanId=addFeature(pd.getPlanClass().toString().toLowerCase(),plansId,false);
        if(pd instanceof DSPLPlanDescription){
            positive=new IntArrayList();
            crossTreeConstraint=((DSPLPlanDescription)pd).getCrossTreeConstraint();
            crossTreeConstraint.addNegativeFeature(thisPlanId);
            addCrossTreeConstraint(crossTreeConstraint);
        }else if(pd instanceof SAPlanDescription){
            // El plan sólo puede aplicarse si tiene algún efecto en la arquitectura.
            negative=new IntArrayList();
            negative.add(thisPlanId);
            for(String included:((SAPlanDescription) pd).getmIncluded()){
                negative.add(getID(included));
            }
            positive=new IntArrayList();
            // Si puede añadir algo que no está añadido
            for(String excluded:((SAPlanDescription) pd).getmExcluded()){
                positive.add(getID(excluded));
            }
            // Si puede eliminar alguna característica
            crossTreeConstraint=new CrossTreeConstraint(positive,negative);
            addCrossTreeConstraint(crossTreeConstraint);
        }else{
            // planes normales.
            thisPlanId=addFeature(pd.getPlanClass().toString().toLowerCase(),plansId,false);
        }
        // Comprobamos que el objetivo no ha sido añadido previamente.
        if(containsFeature(gd.getGoalClass().toString().toLowerCase())){
            // Recuperamos la crosstree constraint asociada a este objetivo
            thisGoalId=getID(gd.getGoalClass().toString().toLowerCase());
            crossTreeConstraint=goalCrossTreeConstraints.get(thisGoalId);
            crossTreeConstraint.addPositiveFeature(thisPlanId);
        }else {
            thisGoalId=addFeature(gd.getGoalClass().toString().toLowerCase(),goalsId,false);
            positive=new IntArrayList();
            positive.add(thisPlanId);
            negative=new IntArrayList();
            negative.add(thisGoalId);
            crossTreeConstraint=new CrossTreeConstraint(positive,negative);
            addCrossTreeConstraint(crossTreeConstraint);
            // registramos esta crosstree constraint para este objetivo
            goalCrossTreeConstraints.put(thisGoalId,crossTreeConstraint);
            Log.d("PREF","Se añade el objetivo "+gd.getGoalClass());
        }
    }

    public void registerService(String id, HashMap<String,StringCrossTreeConstraint> qualityValues, HashMap<String,List<String>> parameterTable, Boolean mandatory){
        // Se añade el servicio al FM
        int thisServiceId=addFeature(id,servicesId,mandatory);

        // se añaden los parámetros
        // raíz de la que cuelgan todos los parámetros
        int thisParameterRootId=addFeature(id+"_parameters",thisServiceId,true);
        // los parámetros.
        Iterator<String> keyIterator=parameterTable.keySet().iterator();
        while (keyIterator.hasNext()){
            String parameter=keyIterator.next();
            int thisParameterId=addFeature(parameter,thisParameterRootId,true);
            List<String> parameterNameList=parameterTable.get(parameter);
            addXORGroup(thisParameterId,parameterNameList);
        }

        // se añaden los valores de calidad de servicio
        int thisServiceQuality=addFeature(id+"_quality",thisServiceId,true);
        String[] qualityValuesArray=new String[qualityValues.keySet().size()];
        List<String> qualityValuesList=new LinkedList<>();
        // se crea una lista con los valores de calidad modificados.
        Iterator<String> qualityValuesIterator=qualityValues.keySet().iterator();
        String qualityValueString;
        while(qualityValuesIterator.hasNext()){
            qualityValueString=qualityValuesIterator.next();
            qualityValuesList.add(id+"_"+qualityValueString);

        }
        //qualityValuesList.addAll(qualityValues.keySet());
        int[] qualityValuesIdArray=addXORGroup(thisServiceQuality,qualityValuesList);

        // se añaden las crosstree constraints asociadas a la calidad.
        qualityValuesIterator=qualityValues.keySet().iterator();
        while(qualityValuesIterator.hasNext()){
            qualityValueString=qualityValuesIterator.next();
            CrossTreeConstraint crossTreeConstraint=qualityValues.get(qualityValueString).getCrossTreeConstraint(this);
            // Está vacía la ctc?
            if(!crossTreeConstraint.getPositiveFeatures().isEmpty() || !crossTreeConstraint.getNegativeFeatures().isEmpty()) {
                // se añade el antecedente de la crosstree constraint
                crossTreeConstraint.addNegativeFeature(getID(id+"_"+qualityValueString));
                addCrossTreeConstraint(crossTreeConstraint);
            }
        }


        /*for(int i=0;i<qualityValuesArray.length;i++){
            // obtenemos la constraint asociada
            CrossTreeConstraint crossTreeConstraint=qualityValues.get(qualityValuesList.get(i)).getCrossTreeConstraint(this);
            // Está vacía la ctc?
            if(!crossTreeConstraint.getPositiveFeatures().isEmpty() || !crossTreeConstraint.getNegativeFeatures().isEmpty()) {
                // se añade el antecedente de la crosstree constraint
                crossTreeConstraint.addNegativeFeature(getID(qualityValuesList.get(i)));
                addCrossTreeConstraint(crossTreeConstraint);
            }
        }*/
    }

    private int[] addXORGroup(int parentId,List<String> group){
        String[] groupValues= group.toArray(new String[group.size()]);
        int index=0;
        int[] res=addFeatureGroup(groupValues,parentId,FeatureModel.TYPE_XOR);
        return res;
    }

    public IntArrayList getGoals(){
        return getChildren(goalsId);
    }

    public IntArrayList getPlans(){
        return getChildren(plansId);
    }

    public IntArrayList getServices(){
        return getChildren(servicesId);
    }

    public IntArrayList getServiceQuality(int serviceId){
        String serviceName=getName(serviceId);
        int serviceQuality=getID(serviceName+"_quality");
        return getChildren(serviceQuality);
    }

    public Boolean isGoal(int id){
        Boolean res=false;
        int parent=getParent(id);
        if(parent==goalsId){
            res=true;
        }
        return res;
    }

    public Boolean isPlan(int id){
        Boolean res=false;
        int parent=getParent(id);
        if(parent==plansId){
            res=true;
        }
        return res;
    }

    public Boolean isServiceRelatedFeature(int id){
        return isService(id) || isServiceParameterRoot(id) || isServiceParameter(id)
                || isServiceParameter(id) || isServiceParameterValue(id)
                || isServiceQualityValue(id) || isServiceQualityRoot(id);
    }

    public Boolean isService(int id){
        Boolean res=false;
        int parent=getParent(id);
        if(parent==servicesId){
            res=true;
        }
        return res;
    }


    // es un parámetro de un servicio, si su ancestro es un elemento "service_parameters"
    public Boolean isServiceParameter(int id){
        int parent=getParent(id);
        return isServiceParameterRoot(parent);
    }


    public Boolean isServiceParameterValue(int id){
        int parent=getParent(id);
        return isServiceParameter(parent);
    }

    // Si tu padre es un servicio y tu nombre termina con parameters
    public Boolean isServiceParameterRoot(int id){
        Boolean res=false;
        int parent=getParent(id);
        String featureName=getName(id);
        if(featureName!=null){
            res=isService(parent);
        }
        /*Boolean res=false;
        IntArrayList serviceList=getServices();
        int index=0;
        while(index<serviceList.size() && !res){
            String serviceName=getName(serviceList.get(index));
            int serviceParameterRoot=getID(serviceName+"_parameters");
            if(id==serviceParameterRoot){
                res=true;
            }
            index++;
        }*/
        return res;
    }

    public Boolean isServiceQualityValue(int id){
        Boolean res=false;
        IntArrayList serviceList=getServices();
        int parent=getParent(id);
        int index=0;
        while(index<serviceList.size() && !res){
            String serviceName=getName(serviceList.get(index));
            int serviceParameterRoot=getID(serviceName+"_quality");
            if(parent==serviceParameterRoot){
                res=true;
            }
            index++;
        }
        return res;
    }

    public Boolean isServiceQualityRoot(int id){
        Boolean res=false;
        IntArrayList serviceList=getServices();
        int index=0;
        while(index<serviceList.size() && !res){
            String serviceName=getName(serviceList.get(index));
            int serviceParameterRoot=getID(serviceName+"_quality");
            if(id==serviceParameterRoot){
                res=true;
            }
            index++;
        }
        return res;
    }

    public int getServiceOfParameterValue(int id){
        int res;
        if(isServiceParameterValue(id)){
            // obtenemos el id del parámetro
            int parameterId=getParent(id);
            // obtenemos el id del "service_parameters"
            int serviceParameterId=getParent(parameterId);
            // se obtiene el id del servicio
            res=getParent(serviceParameterId);
        }else{
            res=-1;
        }
        return res;
    }

    public int getServiceOfQualityValue(int id){
        int res;
        if(isServiceQualityValue(id)){
            // obtenemos el id de quality_value
            int qualityValueId=getParent(id);
            // se obtiene el id del servicio
            res=getParent(qualityValueId);
        }else{
            res=-1;
        }
        return res;
    }

    public Configuration getMinimalConfiguration(){
        Configuration res=new Configuration(this);
        //rootId,sAEngineId,plansId,goalsId,servicesId;
        res.add(rootId);
        res.add(sAEngineId);
        res.add(plansId);
        res.add(goalsId);
        res.add(servicesId);

        return res;
    }

    public void enableService(String servName,Configuration conf){
        // se activan los componentes mínimos para que el servicio sea operativo.
        int servId=getID(servName);
        conf.add(servId);
        int paramId=getID(servName+"_parameters");
        conf.add(paramId);
        int qualityId=getID(servName+"_quality");
        conf.add(qualityId);
    }

    public void setServiceQuality(String servName,String qualityValue,Configuration conf){
        int qualityRootId=getID(servName+"_quality");
        conf.add(qualityRootId);
        int qualityValueId=getID(qualityValue);
        conf.add(qualityValueId);
    }

}
