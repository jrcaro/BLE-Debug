package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class IncreaseUARTPeriodPD extends SAPlanDescription {

    public IncreaseUARTPeriodPD(){
        super();
        setPlanClass(IncreaseUARTPeriodPlan.class);
        List<String> features=new LinkedList<>();
        features.add("10");
        setmIncluded(features);
    }
}
