package com.example.juan.self.plans;

import com.example.juan.tanit.plan.SAPlanDescription;

import java.util.LinkedList;
import java.util.List;

public class SleepSensorsPD extends SAPlanDescription {

    public SleepSensorsPD(){
        super();
        setPlanClass(SleepSensorsPlan.class);
        List<String> includeFeatureList=new LinkedList<>();
        includeFeatureList.add("10");
        includeFeatureList.add("11");
        super.setmIncluded(includeFeatureList);
    }
}
