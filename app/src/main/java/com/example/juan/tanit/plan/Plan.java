package com.example.juan.tanit.plan;

import android.app.Activity;

import java.util.LinkedList;
import java.util.List;

public abstract class Plan {

    private List<Object> arguments;
    private Activity mActivity;

    public Plan() {
        arguments = new LinkedList<>();
    }

    public Activity getmActivity() {
        return mActivity;
    }

    public void setmActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public abstract Boolean execute();

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plan plan = (Plan) o;
        return arguments.equals(plan.arguments) &&
                mActivity.equals(plan.mActivity);
    }

    @Override
    public int hashCode() {
        return arguments.hashCode() + mActivity.hashCode();//Objects.hash(arguments, mActivity);
    }
}
