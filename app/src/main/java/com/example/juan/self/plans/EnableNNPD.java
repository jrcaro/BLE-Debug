package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class EnableNNPD extends SAPlanDescription {

    public EnableNNPD(){
        super();
        setPlanClass(EnableNNPlan.class);
        List<String> featuresToInclude=new LinkedList<>();
        featuresToInclude.add("enable");
        setmIncluded(featuresToInclude);
    }
}
