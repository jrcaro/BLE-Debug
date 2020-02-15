package com.example.juan.self.goals;

import com.example.juan.tanit.selfadaptation.Context;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class GoSleepGD extends GoalDescription {
    public GoSleepGD() {
        super(GoSleepGoal.class);
    }

    @Override
    public Boolean activate(Object input) {
        Context context= (Context)input;
        Boolean repose=(Boolean)context.getValue("repose");
        Boolean awake=(Boolean)context.getValue("awake");
        return repose && awake;
    }
}
