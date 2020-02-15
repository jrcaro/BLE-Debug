package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class DisableNNPD extends SAPlanDescription {

    public DisableNNPD(){
        super();
        setPlanClass(DisableNNPlan.class);
        List<String> features=new LinkedList<>();
        features.add("disable");
        setmIncluded(features);
    }
}
