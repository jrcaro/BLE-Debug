package com.example.juan.self.goals;

import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class SaveEnergyGD extends GoalDescription {
    public SaveEnergyGD() {
        super(SaveEnergyGoal.class);
    }

    @Override
    public Boolean activate(Object input) {
        Context ctx=(Context)input;
        Double batteryLevel=(Double)ctx.getValue("battery");
        return batteryLevel.compareTo(78.0)<=0;
    }
}
