package com.example.juan.ble_debug;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.juan.tanit.selfadaptation.SAComponentInterface;

import java.util.LinkedList;
import java.util.List;

import static com.example.juan.ble_debug.BatteryService.EXTRA_DATA_BATT;

public class BatteryServiceInterface implements SAComponentInterface {

    private MainActivity mainActivity;
    private int quality;
    private Boolean end;
    private Integer period;

    public BatteryServiceInterface(MainActivity ma){
        end=false;
        period =6;
        quality=1;
        mainActivity=ma;
    }

    @Override
    public Boolean putInSafeState() {
        return true;
    }

    @Override
    public Boolean disable() {
        end=false;
        return true;
    }

    @Override
    public Boolean enable() {
        end=true;
        return true;
    }

    @Override
    public Boolean tuneParameter(String key, Object param) {
        Log.d("cane",key+" "+param.toString());
        if(key.equals("battery_period")){
            // el param puedes hacer casting a Double
            period =Integer.parseInt((String)param);
            Log.d("cane","La frecuencia de la batería esta en "+ period);
            mainActivity.actionChangeBatteryPeriod(new Double(period));
        }else if(key.equals("battery_quality")){
            String qualityString=(String)param;
            if(qualityString.endsWith("1")){
                quality=1;
            }else if(qualityString.endsWith("2")){
                quality=2;
            }
            Log.d("cane","La calidad de la batería esta en "+quality);
        }
        return true;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }
}
