package com.example.juan.self.goals;

import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class WakeUpGD extends GoalDescription {
    public WakeUpGD() {
        super(WakeUpGoal.class);
    }

    @Override
    public Boolean activate(Object input) {
        Context context=(Context)input;
        Boolean repose=(Boolean)context.getValue("repose");
        Double batteryLevel=(Double)context.getValue("battery");
        Boolean awake=(Boolean)context.getValue("awake");
        return !repose && batteryLevel.compareTo(30.0)>0 && !awake;
    }
}
