package com.example.juan.tanit.selfadaptation;

import android.util.Log;

import com.carrotsearch.hppc.IntArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.example.juan.tanit.dspl.Configuration;
import com.example.juan.tanit.dspl.PreferenceBasedFeatureModel;

public class PreferenceUpdater extends Thread {

    private PreferenceBasedEngine engine;
    private Double wellness,usefulness;
    private HashMap<String, WellnessFactor> wellnessFactorList;
    private HashMap<String,Double> currentWellnessFactor;
    private Boolean end;

    public PreferenceUpdater(PreferenceBasedEngine pbe){
        engine=pbe;
        wellness=0.0;
        usefulness=0.0;
        wellnessFactorList =new LinkedHashMap<>();
        currentWellnessFactor=new LinkedHashMap<>();
        end=false;
    }

    // los valores de usefulness and wellness se computan de manera periódica.
    @Override
    public void run() {

        while (!end) {
            wellness = computeWellness();
            usefulness= computeCurrentUsefulness();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("PREF","Wellness: "+wellness+" Usefulness: "+usefulness);
        }
    }

    public void finishService(){
        end=true;
    }

    // wellness está relacionado con la condición del agente respecto a los factores de auto-gestión.
    private Double computeWellness(){
        Double res=0.0;

        Iterator<String> iterator=wellnessFactorList.keySet().iterator();
        String key;
        while(iterator.hasNext()){
            key=iterator.next();
            WellnessFactor wf=wellnessFactorList.get(key);
            Double currentWellnessValue=currentWellnessFactor.get(key);
            res=res+wf.computeWellness(currentWellnessValue);
        }
        return res;
    }

    // el valor se maximiza o se minimiza
    protected void addWellnessFactor(String id, Optimization m,Double crit_value){
        WellnessFactor wf=new WellnessFactor(id,m,crit_value);
        wellnessFactorList.put(id,wf);
    }

    protected void addWellnessFactor(WellnessFactor wellnessFactor){
        wellnessFactorList.put(wellnessFactor.getId(),wellnessFactor);
    }

    protected void updateWellnessFactor(String id,Double val){
        currentWellnessFactor.put(id,val);
    }


    private Double computeCurrentUsefulness(){
        return computeUsefulness(engine.getCurrentConfiguration());
    }

    public Double computeUsefulness(Configuration conf){
        Double res=0.0;
        PreferenceBasedFeatureModel pbFeatureModel=(PreferenceBasedFeatureModel) engine.getFeatureModel();

        IntArrayList goals=pbFeatureModel.getGoals();

        int activeGoals=0;
        for(int i=0;i<goals.size();i++){
            if(conf.contains(goals.get(i))){
                activeGoals++;
            }
        }
        int goalValue=activeGoals/goals.size();

        int serviceQuality=0;
        IntArrayList serviceList=pbFeatureModel.getServices();
        for(int i=0;i<serviceList.size();i++){
            IntArrayList serviceQualityList=pbFeatureModel.getServiceQuality(serviceList.get(i));

            for(int q=0;q<serviceQualityList.size();q++){
                if(conf.contains(serviceQualityList.get(q))){
                    String qualityLabel=pbFeatureModel.getName(serviceQualityList.get(q));
                    if(qualityLabel.endsWith("1")){
                        serviceQuality=serviceQuality+100;
                    }else if(qualityLabel.endsWith("2")){
                        serviceQuality=serviceQuality+66;
                    }else{
                        serviceQuality=serviceQuality+33;
                    }
                }
            }
        }
        res=serviceQuality/new Double(serviceList.size()*100);
        return res;
    }

    public Double getWellness() {
        return wellness;
    }

    public Double getUsefulness() {
        return usefulness;
    }

    public int getNumberOfWellnessFactor(){
        return wellnessFactorList.keySet().size();
    }

    public Double getCurrentWellnessState(String id){
        return currentWellnessFactor.get(id);
    }

    public HashMap<String,Double> getCurrentWellnessFactor(){
        return currentWellnessFactor;
    }
}
