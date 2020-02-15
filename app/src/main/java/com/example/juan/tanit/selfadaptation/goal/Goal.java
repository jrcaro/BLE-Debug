package com.example.juan.tanit.selfadaptation.goal;

import java.util.LinkedList;
import java.util.List;

import com.example.juan.tanit.plan.Plan;
import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.SelfAdaptationEngine;

public abstract class Goal {
    private List<Plan> attempts;
    private SelfAdaptationEngine engine;

    public Goal(){
        attempts=new LinkedList<>();
    }

    public void setEngine(SelfAdaptationEngine e){
        engine=e;
    }

    public Context getContext(){
        return engine.getContext();
    }

    public void registerAttempt(Plan p){
        attempts.add(p);
    }

    public List<Plan> getAttempts(){
        return attempts;
    }

    public Boolean tried(Class planClass){
        Boolean res=false;
        int index=0;

        while(index<attempts.size() && !res){
            Plan attempt=attempts.get(index);
            if(planClass.equals(attempt.getClass())){
                res=true;
            }
            index++;
        }
        return res;
    }

    public abstract Boolean accomplished();
}
