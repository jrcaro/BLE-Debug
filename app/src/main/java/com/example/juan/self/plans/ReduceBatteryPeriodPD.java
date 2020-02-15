package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class ReduceBatteryPeriodPD extends SAPlanDescription {

    public ReduceBatteryPeriodPD(){
        super();
        setPlanClass(ReduceBatteryPeriodPlan.class);
        List<String> features=new LinkedList<String>();
        features.add("6");
        setmIncluded(features);
    }
}
