package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class ReduceUARTPeriodPD extends SAPlanDescription {

    public ReduceUARTPeriodPD(){
        super();
        setPlanClass(ReduceUARTPeriodPlan.class);
        List<String> features=new LinkedList<>();
        features.add("5");
        setmIncluded(features);
    }
}
