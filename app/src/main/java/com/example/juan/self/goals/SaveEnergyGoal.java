package com.example.juan.self.goals;

import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.goal.Goal;

public class SaveEnergyGoal extends Goal {

    @Override
    public Boolean accomplished() {
        Context context=getContext();
        Double batteryLevel=(Double) context.getValue("battery");
        return batteryLevel>78;
    }
}
