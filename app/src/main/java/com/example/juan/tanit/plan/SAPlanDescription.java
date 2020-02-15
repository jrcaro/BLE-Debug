package com.example.juan.tanit.plan;


import android.util.Log;

import com.carrotsearch.hppc.IntArrayList;

import java.util.LinkedList;
import java.util.List;

import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.FeatureModel;

public class SAPlanDescription extends PlanDescription {

    private List<String> mExcluded,mIncluded;

    public SAPlanDescription() {
        super(SelfAdaptationPlan.class);
        mExcluded=new LinkedList<>();
        mIncluded=new LinkedList<>();
    }

    public SAPlanDescription(List<String> mIncl,List<String> mExc){
        this();
        mIncluded=mIncl;
        mExcluded=mExc;
    }

    @Override
    public Boolean checkPreCondition(Object object) {
        Boolean res=false;
        if(object instanceof Configuration){
            // una configuración es una lista de números enteros
            Configuration conf=(Configuration)object;
            FeatureModel fm=conf.getFeatureModel();
            IntArrayList currentConf=conf.getConfigurationByID();

            // todas las características a incluir de este plan se encuentran incluidas en la configuración actual.
            int  index=0;
            while(index < mIncluded.size() && !res){
                if(!currentConf.contains(fm.getID(mIncluded.get(index)))){
                    res=true;
                }
                index++;
            }
            // todas las características a excluir de este plan se encuentran incluidas en la configuración actual.
            if(!res){
                index=0;
                while(index < mExcluded.size() && !res){
                    if(currentConf.contains(fm.getID(mExcluded.get(index)))){
                        res=true;
                    }
                    index++;
                }
            }
        }
        if(res){
            Log.d("Self","El plan "+getPlanClass().getName()+" puede aplicarse");
        }else{
            Log.d("Self","El plan "+getPlanClass().getName()+" NO puede aplicarse");
        }
        return res;
    }

    @Override
    public Plan instantiatePlan() {
        SelfAdaptationPlan saPlan=(SelfAdaptationPlan)super.instantiatePlan();
        saPlan.setmExcluded(mExcluded);
        saPlan.setmIncluded(mIncluded);
        return saPlan;
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
}
