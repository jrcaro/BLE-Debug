package com.example.juan.tanit.selfadaptation.goal;

public abstract class GoalDescription {

    private Class goalClass;

    public GoalDescription(Class gc) {
        goalClass = gc;
    }

    public abstract Boolean activate(Object input);

    public Class getGoalClass() {
        return goalClass;
    }

    public void setGoalClass(Class goalClass) {
        this.goalClass = goalClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalDescription that = (GoalDescription) o;
        return goalClass.equals(that.goalClass);
    }

    @Override
    public int hashCode() {
        return goalClass.hashCode();
    }
}
