package com.example.juan.ble_debug;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.juan.tanit.selfadaptation.SAComponentInterface;

public class UARTServiceInterface implements SAComponentInterface {

    private MainActivity mainActivity;
    private Boolean end;
    private int quality;
    private Integer period;
    private Boolean neuralNetwork;

    public UARTServiceInterface(MainActivity ma){
        mainActivity=ma;
        end=false;
        period =6;
        quality=1;
        neuralNetwork=true;
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
        Log.d("cane","Tune parameter de UART "+key+" "+param.toString());
        if(key.equals("uart_period")){
            // el param puedes hacer casting a Double
            period =Integer.parseInt((String)param);
            Log.d("cane","La frecuencia de la uart esta en "+ period);
            mainActivity.actionChangeUartPeriod(new Double(period));
        }else if(key.equals("uart_quality")){
            String qualityString=(String)param;
            if(qualityString.endsWith("1")){
                quality=1;
            }else if(qualityString.endsWith("2")){
                quality=2;
            }else if(qualityString.endsWith("3")){
                quality=3;
            }
            Log.d("cane","La calidad de la uart esta en "+quality);
        }else if(key.equals("nn")){
            String state=(String)param;
            if(state.equals("enable")){
                neuralNetwork=true;
                Log.d("cane","NN activado");
            }else{
                neuralNetwork=false;
                Log.d("cane","NN desactivado");
            }
            mainActivity.actionChangeNN(neuralNetwork);
        }

        return true;
    }
    //


    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Boolean getNeuralNetwork() {
        return neuralNetwork;
    }

    public void setNeuralNetwork(Boolean neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }
}
