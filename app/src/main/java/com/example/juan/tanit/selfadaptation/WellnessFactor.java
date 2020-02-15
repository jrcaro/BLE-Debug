package com.example.juan.tanit.selfadaptation;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.Objects;

public class WellnessFactor {
    private String id;
    private Optimization direction;
    private Double peakValue;

    public WellnessFactor(String i,Optimization d,Double pv){
        id=i;
        direction=d;
        peakValue=pv;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optimization getDirection() {
        return direction;
    }

    public void setDirection(Optimization direction) {
        this.direction = direction;
    }

    public Double getPeakValue() {
        return peakValue;
    }

    public void setPeakValue(Double peakValue) {
        this.peakValue = peakValue;
    }

    public Double computeWellness(Double currentValue){
        Double res=0.0;

        // si es un valor a maximizar
        if(this.getDirection()==Optimization.MAX){
            // el valor normalizado es el valor actual/max
            res=res+(currentValue/this.getPeakValue());
            // si es un valor a minimizar transformamos el problema.
        }else{
            res=res+((2*this.getPeakValue()-currentValue)/this.getPeakValue());
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WellnessFactor that = (WellnessFactor) o;
        return id.equals(that.id) &&
                direction == that.direction &&
                peakValue.equals(that.peakValue);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(id, direction, peakValue);
    }
}
