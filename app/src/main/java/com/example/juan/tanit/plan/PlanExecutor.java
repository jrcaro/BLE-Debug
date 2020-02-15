package com.example.juan.tanit.plan;

import android.app.Activity;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.example.juan.tanit.selfadaptation.SelfAdaptationEngine;

public class PlanExecutor extends Thread {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private List<Plan> planQueue;
    private Integer waitingTime;
    private Boolean end;

    // Reference to the activity that has launched this executor.
    private Activity mActivity;

    private SelfAdaptationEngine saEngine;

    public PlanExecutor(Activity a,SelfAdaptationEngine sa) {
        planQueue = new LinkedList<>();
        waitingTime = 100;
        end = false;
        mActivity = a;
        saEngine=sa;
    }

    public void cancelService() {
        end = true;
    }

    private void tuneExecutor(Integer time) {
        waitingTime = time;
    }

    public void addPlan(Plan p) {
        writeLock.lock();
        planQueue.add(p);
        writeLock.unlock();
    }

    public Plan getPlanForExecution() {
        Plan res = null;
        writeLock.lock();
        if (planQueue.size() > 0) {
            res = planQueue.get(0);
            planQueue.remove(0);
        }
        writeLock.unlock();
        return res;
    }

    public Boolean pendingPlans() {
        Boolean res = false;
        readLock.lock();
        res = planQueue.size() > 0;
        readLock.unlock();
        return res;
    }

    @Override
    public void run() {

        while (!end) {
            if (pendingPlans()) {
                Plan p = getPlanForExecution();
                p.setmActivity(mActivity);
                Log.d("Self","Se ejecuta "+p.getClass().toString());
                if(!p.execute()){
                    saEngine.planFailed(p);
                }else{
                    saEngine.planSucceed(p);
                }
            } else {
                try {
                    Thread.sleep(waitingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void changeExecutionContext(Activity act){
        mActivity=act;
    }


}
