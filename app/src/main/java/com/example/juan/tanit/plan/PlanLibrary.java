package com.example.juan.tanit.plan;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.example.juan.tanit.selfadaptation.goal.Goal;
import com.example.juan.tanit.selfadaptation.goal.GoalDescription;

public class PlanLibrary {

    private HashMap<GoalDescription, List<PlanDescription>> library;

    public PlanLibrary() {
        library = new HashMap<>();
    }

    public List<PlanDescription> getPlan(Goal g) {
        List<PlanDescription> res = new LinkedList<>();
        Iterator<GoalDescription> goalIterator = library.keySet().iterator();
        Boolean success = false;

        while (!success && goalIterator.hasNext()) {
            GoalDescription gd = goalIterator.next();
            if (gd.getGoalClass().equals(g.getClass())) {
                res = library.get(gd);
                success = true;
            }
        }
        return res;
    }

    public void removeEntry(GoalDescription goalDescription) {
        library.remove(goalDescription);
    }

    public void removeEntry(GoalDescription goalDescription, PlanDescription pd) {
        List<PlanDescription> planDescriptionList;
        if (library.containsKey(goalDescription)) {
            planDescriptionList = library.get(goalDescription);
            Boolean sucess = false;
            int index = 0;
            while (index < planDescriptionList.size() && !sucess) {
                if (planDescriptionList.get(index).equals(pd)) {
                    planDescriptionList.remove(index);
                }
            }
            if (planDescriptionList.isEmpty()) {
                library.remove(goalDescription);
            }
        }
    }

    public void registerPlanDescription(GoalDescription gd, List<PlanDescription> pdList) {
        if (library.containsKey(gd)) {
            List<PlanDescription> currentPDList = library.get(gd);
            currentPDList.addAll(pdList);
        } else {
            library.put(gd, pdList);
        }
    }

    public void registerPlanDescription(GoalDescription gd, PlanDescription pd) {
        List<PlanDescription> pdList;
        // Se ha registrado este objetivo?
        if (library.containsKey(gd)) {
            pdList = library.get(gd);
            pdList.add(pd);
        } else {
            pdList = new LinkedList<>();
            pdList.add(pd);
            library.put(gd, pdList);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanLibrary that = (PlanLibrary) o;
        return Objects.equals(library, that.library);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(library);
    }
}
