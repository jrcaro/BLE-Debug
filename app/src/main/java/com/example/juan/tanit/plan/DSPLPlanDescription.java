package com.example.juan.tanit.plan;


import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.CrossTreeConstraint;

public class DSPLPlanDescription extends PlanDescription {

    private CrossTreeConstraint crossTreeConstraint;

    public DSPLPlanDescription(Class pc, CrossTreeConstraint ctt) {
        super(pc);
        crossTreeConstraint=ctt;
    }

    @Override
    public Boolean checkPreCondition(Object object) {
        Boolean res=false;
        if(object instanceof Configuration){
            Configuration conf=(Configuration)object;
            res=crossTreeConstraint.isSatisfied(conf);
        }
        return res;
    }

    public CrossTreeConstraint getCrossTreeConstraint() {
        return crossTreeConstraint;
    }
}
