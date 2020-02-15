package com.example.juan.tanit.plan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.example.juan.tanit.selfadaptation.goal.Goal;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class PreferenceBasedPlanLibrary extends PlanLibrary {

    private HashMap<GoalDescription, List<PlanPreference>> planWellnessLibrary;

    public PreferenceBasedPlanLibrary(){
        super();
        planWellnessLibrary=new LinkedHashMap<>();
    }

    public List<PlanPreference> getPlanWellness(Goal g) {
        List<PlanPreference> res = new LinkedList<>();
        Iterator<GoalDescription> goalIterator = planWellnessLibrary.keySet().iterator();
        Boolean success = false;

        while (!success && goalIterator.hasNext()) {
            GoalDescription gd = goalIterator.next();
            if (gd.getGoalClass().equals(g.getClass())) {
                res = planWellnessLibrary.get(gd);
                success = true;
            }
        }
        return res;
    }

    @Override
    public void removeEntry(GoalDescription goalDescription) {
        super.removeEntry(goalDescription);
        // eliminar de la otra estructura.
        planWellnessLibrary.remove(goalDescription);
    }

    @Override
    public void removeEntry(GoalDescription goalDescription, PlanDescription pd) {
        super.removeEntry(goalDescription, pd);

        List<PlanPreference> planPreferenceList =planWellnessLibrary.get(goalDescription);
        Boolean success=false;
        int index=0;

        while(index < planPreferenceList.size() && !success){
            if(pd.equals(planPreferenceList.get(index).getPlanDescription())){
                success=true;
                planPreferenceList.remove(index);
                // si ya no quedan planes asociados a ese objetivo lo borramos de la lista
                if(planPreferenceList.isEmpty()){
                    planWellnessLibrary.remove(goalDescription);
                }
            }
            index++;
        }
    }

    public void registerPlanWellness(GoalDescription gd, List<PlanPreference> planPreferenceList){
        if (planWellnessLibrary.containsKey(gd)) {
            List<PlanPreference> currentPDList = planWellnessLibrary.get(gd);
            currentPDList.addAll(planPreferenceList);
        } else {
            planWellnessLibrary.put(gd, planPreferenceList);
        }
    }

    public void registerPlanWellness(GoalDescription gd, PlanPreference pw){
        List<PlanPreference> pdList;
        // Se ha registrado este objetivo?
        if (planWellnessLibrary.containsKey(gd)) {
            pdList = planWellnessLibrary.get(gd);
            pdList.add(pw);
        } else {
            pdList = new LinkedList<>();
            pdList.add(pw);
            planWellnessLibrary.put(gd, pdList);
        }
    }


}
