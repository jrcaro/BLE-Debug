package com.example.juan.tanit.dspl;

import java.util.Random;

public class ConfigurationFactory {

    public Configuration getRandomConfiguration(FeatureModel fm) {
        Random normalRandom=new Random(System.currentTimeMillis());
        //XSRandom localXsRandom=new XSRandom(System.currentTimeMillis());
        // we select the number of features that will be enabled.
        int number_of_enabled_features=normalRandom.nextInt(fm.size());
        int counter=0;
        int candidate;
        Configuration config=null;
        BasicFix fix=new BasicFix();

        System.out.println("Inicialmente se activan "+number_of_enabled_features+" características.");

        boolean[] booleanConfig = new boolean[fm.size()];
        // reiniciamos la configuración a valores falsos.
        for(boolean cell:booleanConfig){
            cell=false;
        }

        while (counter < number_of_enabled_features) {
            candidate = normalRandom.nextInt(fm.size());
            if (!booleanConfig[candidate]) {
                booleanConfig[candidate] = true;
                counter++;
            }
        }
        config = new Configuration(fm, booleanConfig);
        while (!(config.checkCrossTreeConstraints() && config.checkTreeConstraints())){
            Configuration fixedConfiguration=new Configuration(fm);
            fix.fix(config,fixedConfiguration);
            config=fixedConfiguration;
        }
        return config;
    }


}
