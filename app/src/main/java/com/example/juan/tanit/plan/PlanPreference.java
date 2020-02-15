package com.example.juan.tanit.plan;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import com.example.juan.tanit.selfadaptation.Optimization;
import com.example.juan.tanit.selfadaptation.WellnessFactor;

public class PlanPreference {
    private PlanDescription planDescription;
    private HashMap<WellnessFactor,Double> planEffect;
    private Double planQuality; // a value between 0-1 that reflects the quality a of a plan to accomplish a goal.

    public PlanPreference(PlanDescription pd, HashMap<WellnessFactor,Double> pc, Double pq){
        planDescription=pd;
        planEffect =pc;
        planQuality=pq;
    }

    public Double computePlanEffect(HashMap<String,Double> currentWellness){
        HashMap<String,Double> notNormalizedEffect=new HashMap<>();
        Iterator<WellnessFactor> iterator= planEffect.keySet().iterator();
        WellnessFactor key;

        while (iterator.hasNext()){
            key=iterator.next();
            Double current=currentWellness.get(key.getId());
            Double effect=current - planEffect.get(key);
            notNormalizedEffect.put(key.getId(),effect);
            if (key.getDirection()== Optimization.MIN){
                if(effect<key.getPeakValue()){
                    effect=key.getPeakValue();
                }
            }else if(key.getDirection()== Optimization.MAX){
                if(effect>key.getPeakValue()){
                    effect=key.getPeakValue();
                }
            }
            notNormalizedEffect.put(key.getId(),effect);
        }
        HashMap<String,Double> normalizedEffect=normalizePlanEffect(notNormalizedEffect);
        // Se suma el vector para tener el valor de wellness
        Double res=0.0;
        Iterator<String> idIterator=normalizedEffect.keySet().iterator();
        while(idIterator.hasNext()){
            res=res+normalizedEffect.get(idIterator.next());
        }

        return res;
    }

    private HashMap<String,Double>  normalizePlanEffect(HashMap<String,Double> pe){
        HashMap<String,Double> normalizedPE=new HashMap<>();

        Iterator<WellnessFactor> iterator=planEffect.keySet().iterator();
        WellnessFactor wellnessFactor;

        while (iterator.hasNext()){
            wellnessFactor=iterator.next();
            Double notNormalizedEffect=pe.get(wellnessFactor.getId());
            Double normalizedEffect=0.0;
            if(wellnessFactor.getDirection()==Optimization.MAX){
                normalizedEffect=notNormalizedEffect/wellnessFactor.getPeakValue();
            }else if (wellnessFactor.getDirection()==Optimization.MIN){
                normalizedEffect=(2*wellnessFactor.getPeakValue()-notNormalizedEffect)/wellnessFactor.getPeakValue();
            }
            normalizedPE.put(wellnessFactor.getId(),normalizedEffect);
        }
        return normalizedPE;
    }

    public PlanDescription getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(PlanDescription planDescription) {
        this.planDescription = planDescription;
    }

    public HashMap<WellnessFactor, Double> getPlanEffect() {
        return planEffect;
    }

    public void setPlanEffect(HashMap<WellnessFactor, Double> planEffect) {
        this.planEffect = planEffect;
    }

    public Double getPlanQuality() {
        return planQuality;
    }

    public void setPlanQuality(Double planQuality) {
        this.planQuality = planQuality;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanPreference that = (PlanPreference) o;
        return Objects.equals(planDescription, that.planDescription) &&
                Objects.equals(planEffect, that.planEffect) &&
                Objects.equals(planQuality, that.planQuality);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(planDescription, planEffect, planQuality);
    }
}
