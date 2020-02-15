package com.example.juan.self.goals;

import android.util.Log;

import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class IncreasePrecisionGD extends GoalDescription {
    public IncreasePrecisionGD() {
        super(IncreasePrecisionGoal.class);
    }

    @Override
    public Boolean activate(Object input) {
        Boolean res=false;
        Context context=(Context)input;
        Double batteryLevel=(Double)context.getValue("battery");
        //Log.d("Self","El valor de la batería es "+batteryLevel);
        if(batteryLevel.compareTo(82.0)>0){
            res=true;
        }
        return res;
    }
}
