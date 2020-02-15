package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class IncreaseBatteryPeriodPD extends SAPlanDescription {
    public IncreaseBatteryPeriodPD(){
        super();
        setPlanClass(IncreaseBatteryPeriodPlan.class);
        List<String> features=new LinkedList<>();
        features.add("11");
        setmIncluded(features);
    }
}
