package com.example.juan.tanit.plan;

import android.util.Log;

import com.example.juan.tanit.dspl.PreferenceBasedFeatureModel;
import com.example.juan.tanit.selfadaptation.SAActivityInterface;
import com.example.juan.tanit.selfadaptation.SAComponentInterface;


public class SSAdaptationPlan extends SelfAdaptationPlan {

    // La lista de incluir y excluir elementos puede incluir elementos de distintos tipos
    // Hay que distinguir el tipo de característica que se está materializando
    @Override
    public Boolean execute() {

        //Log.d("Self","La configuración resultante deber ser: ");
        //Log.d("Self",getNewConfiguration().toString());
        Log.d("Self","Se aplica el siguiente plan: ");
        Log.d("Self","Se desactivan los siguientes componentes: ");
        for(int i=0;i<getToExclude().size();i++){
            Log.d("Self",getToExclude().get(i));
        }
        Log.d("Self","Se activan los siguientes componentes:");
        for(int i=0;i<getToInclude().size();i++){
            Log.d("Self",getToInclude().get(i));
        }

        Boolean success=true;
        int index=0;
        SAActivityInterface activity=(SAActivityInterface)getmActivity();
        SAComponentInterface component;
        PreferenceBasedFeatureModel pbFeatureModel=(PreferenceBasedFeatureModel)
                getNewConfiguration().getFeatureModel();

        // se desactivan los componentes
        Log.d("Self","Se desactivan componentes.");
        while(index < getToExclude().size() && success){
            int featureId=pbFeatureModel.getID(getToExclude().get(index));
            if(pbFeatureModel.isService(featureId)){
                Log.d("Self","Se desactiva "+getToExclude().get(index));
                // solo se tiene que actuar en caso de que se deshabilite un servicio
                component = activity.getComponent(getToExclude().get(index));
                if (component != null) {
                    success = component.putInSafeState();
                    if (success) {
                        success = component.disable();
                    }
                } else {
                    Log.d("Self", "El plan ha fallado porque no se ha encontrado el componente " + getToExclude().get(index));
                    success=false;
                }
            }
            index++;
        }
        // se activan los componentes
        if(success) {
            Log.d("Self","Se activan componentes.");
            index = 0;
            while(index<getToInclude().size() && success){
                int featureToIncludeId=pbFeatureModel.getID(getToInclude().get(index));
                //Log.d("Self","Se intenta activar el componente "+getToInclude().get(index));
                if(pbFeatureModel.isService(featureToIncludeId)){
                    Log.d("Self","Se activa el servicio "+getToInclude().get(index));
                    component = activity.getComponent(getToInclude().get(index));
                    if (component != null) {
                        success = component.enable();
                    } else {
                        Log.d("Self", "El plan ha fallado porque no se ha encontrado el servicio " + getToInclude().get(index));
                        success=false;
                    }
                }else if(pbFeatureModel.isServiceParameterValue(featureToIncludeId)){
                    Log.d("Self","Se activa el parámetro de servicio "+getToInclude().get(index));
                    // se obtiene el nombre del servicio "padre"
                    int serviceId=pbFeatureModel.getServiceOfParameterValue(featureToIncludeId);
                    if(serviceId==-1){
                        Log.d("Self","No se ha encontrado en el FM servicio asociado al parámetro");
                        success=false;
                    }else {
                        String serviceName = pbFeatureModel.getName(serviceId);
                        component = activity.getComponent(serviceName);
                        // se obtiene el nombre del parámetro
                        int parameterId = pbFeatureModel.getParent(featureToIncludeId);
                        if(parameterId==-1){
                            Log.d("Self","El servicio "+serviceName+" no está registrado en la arquitectura.");
                            success=false;
                        }else {
                            String parameterName = pbFeatureModel.getName(parameterId);
                            success=component.tuneParameter(parameterName, getToInclude().get(index));

                        }
                    }
                } else if (pbFeatureModel.isServiceQualityValue(featureToIncludeId)) {
                    Log.d("Self","Se añade la calidad de servicio "+getToInclude().get(index));
                    // se obtiene el nombre del servicio "padre"
                    int serviceId = pbFeatureModel.getServiceOfQualityValue(featureToIncludeId);
                    if (serviceId == -1) {
                        success = false;
                    } else {
                        String serviceName = pbFeatureModel.getName(serviceId);
                        component = activity.getComponent(serviceName);
                        success = component.tuneParameter("quality", getToInclude().get(index));

                    }
                }
                index++;
            }
        }
        return success;
    }
}
