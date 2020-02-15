package com.example.juan.tanit.plan;

public class PlanDescription {

    private Class planClass;

    public PlanDescription(Class pc) {
        planClass = pc;
    }

    public Boolean checkPreCondition(Object object){
        return true;
    }

    public Class getPlanClass() {
        return planClass;
    }

    public void setPlanClass(Class planClass) {
        this.planClass = planClass;
    }

    public Plan instantiatePlan(){
        Plan res=null;
        try {
            res=(Plan)planClass.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanDescription that = (PlanDescription) o;
        return planClass.equals(that.planClass);
    }

    @Override
    public int hashCode() {
        return planClass.hashCode();
    }
}
