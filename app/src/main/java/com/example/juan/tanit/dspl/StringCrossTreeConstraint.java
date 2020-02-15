package com.example.juan.tanit.dspl;

import com.carrotsearch.hppc.IntArrayList;

import java.util.List;

public class StringCrossTreeConstraint {

    private List<String> positive,negative;

    public StringCrossTreeConstraint(List<String> pos,List<String> neg){
        positive=pos;
        negative=neg;
    }

    public CrossTreeConstraint getCrossTreeConstraint(FeatureModel fm){
        IntArrayList positiveIntList=new IntArrayList();
        IntArrayList negativeIntList=new IntArrayList();

        for(String feature:positive){
            positiveIntList.add(fm.getID(feature));
        }
        for(String feature:negative){
            negativeIntList.add(fm.getID(feature));
        }
        CrossTreeConstraint res=new CrossTreeConstraint(positiveIntList,negativeIntList);
        return res;
    }
}
