package com.example.juan.tanit.plan;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.carrotsearch.hppc.IntArrayList;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.FeatureModel;
import com.example.juan.tanit.selfadaptation.SAActivityInterface;
import com.example.juan.tanit.selfadaptation.SAComponentInterface;

public class SelfAdaptationPlan extends Plan {

    private List<String> mExcluded,mIncluded,toExclude,toInclude;
    private Configuration newConfiguration;

    public SelfAdaptationPlan(){
        mExcluded=new LinkedList<>();
        mIncluded=new LinkedList<>();
        toExclude=new LinkedList<>();
        toInclude=new LinkedList<>();
    }

    public SelfAdaptationPlan(List<String> me,List<String> mi){
        mExcluded=me;
        mIncluded=mi;
        toInclude=new LinkedList<>();
        toInclude.addAll(mIncluded);
        toExclude=new LinkedList<>();
        toExclude.addAll(mExcluded);
    }

    public void setNewConfiguration(Configuration conf){
        newConfiguration=conf;
    }

    public Configuration getNewConfiguration(){
        return newConfiguration;
    }

    public List<String> getmExcluded() {
        return mExcluded;
    }

    public void setmExcluded(List<String> mExcluded) {
        this.mExcluded = mExcluded;
    }

    public List<String> getmIncluded() {
        return mIncluded;
    }

    public void setmIncluded(List<String> mIncluded) {
        this.mIncluded = mIncluded;
    }

    public List<String> getToExclude() {
        return toExclude;
    }

    public void setToExclude(List<String> toExclude) {
        this.toExclude = toExclude;
    }

    public List<String> getToInclude() {
        return toInclude;
    }

    public void setToInclude(List<String> toInclude) {
        this.toInclude = toInclude;
    }

    public void addToIncludeFeature(String feature){
        toInclude.add(feature);
    }

    public void removeToIncludeFeature(String feature){
        toInclude.remove(feature);
    }

    public void addToRemoveFeature(String feature){
        toExclude.add(feature);
    }

    public void removeToExcludeFeature(String feature){
        toExclude.remove(feature);
    }

    public Boolean checkPrecondition(Configuration conf){
        Log.d("Self","se comprueba la pre-condición.");
        Boolean res=false;
        FeatureModel fm=conf.getFeatureModel();
        IntArrayList currentConf=conf.getConfigurationByID();

        // todas las características a incluir de este plan se encuentran incluidas en la configuración actual.
        int  index=0;
        while(index < this.getmIncluded().size() && !res){
            if(!currentConf.contains(fm.getID(this.getmIncluded().get(index)))){
                res=true;
            }
            index++;
        }
        // todas las características a excluir de este plan se encuentran incluidas en la configuración actual.
        if(!res){
            index=0;
            while(index < this.getmExcluded().size() && !res){
                if(currentConf.contains(fm.getID(this.getmExcluded().get(index)))){
                    res=true;
                }
                index++;
            }
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SelfAdaptationPlan that = (SelfAdaptationPlan) o;
        return mExcluded.equals( that.mExcluded) &&
                mIncluded.equals( that.mIncluded) &&
                toExclude.equals( that.toExclude) &&
                toInclude.equals( that.toInclude);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mExcluded, mIncluded, toExclude, toInclude);
    }

    // la ejecución del plan consiste en habilitar y deshabilitar los componentes que se indican.
    // Si uno sólo de los componentes que tienen que habilitarse o deshabilitarse no pueden obtenerse, el  plan se considera fallido
    @Override
    public Boolean execute() {

        Log.d("Self","Se ejecuta un plan de auto-adaptación.");

        Boolean success=true;
        Boolean failed=false;
        int index=0;
        SAActivityInterface activity=(SAActivityInterface)getmActivity();
        SAComponentInterface component;

        // desactivamos los componentes
        while(index < toExclude.size() && success && !failed){
            component=activity.getComponent(toExclude.get(index));
            if(component!=null) {
                success = component.putInSafeState();
                if (success) {
                    success = component.disable();
                }
            }else{
                Log.d("Self","El plan ha fallado porque no se ha encontrado el componente "+toExclude.get(index));
                failed=true;
            }
            index++;
        }
        if(!failed) {
            if (success) {
                index = 0;
                while (index < toInclude.size() && success && !failed) {
                    component = activity.getComponent(toInclude.get(index));
                    if(component!=null) {
                        success = component.enable();
                    }else{
                        failed=true;
                    }
                    index++;
                }
            }
            return success;
        }else{
            return false;
        }
    }
}
