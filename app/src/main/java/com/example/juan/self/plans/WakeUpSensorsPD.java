package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class WakeUpSensorsPD extends SAPlanDescription {

    public WakeUpSensorsPD(){
        super();
        setPlanClass(WakeUpSensorsPlan.class);
        List<String> includeFeatureList=new LinkedList<>();
        includeFeatureList.add("5");
        includeFeatureList.add("6");
        super.setmIncluded(includeFeatureList);
    }
}
