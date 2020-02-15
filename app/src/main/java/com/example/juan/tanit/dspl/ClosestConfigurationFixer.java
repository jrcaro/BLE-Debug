package com.example.juan.tanit.dspl;

import android.util.Log;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;

import java.util.List;

/**
 * Obtiene la configuración más parecida a la original que contiene y excluye las características indicadas.
 * Es obligatorio que contenga o excluya esas características.
 * La configuración que se recibe como entrada ya contiene y excluye esas características, pero puede ser incorrrecta.
 * **/

public class ClosestConfigurationFixer {

    private FeatureModel mFeatureModel;
    private IntArrayList mExcludedFeatures,mIncludedFeatures;

    private int[][] mPrebuiltArrays;
    private static final int PREBUILT_MAX = 50;
    private XSRandom mRandom = new XSRandom(System.currentTimeMillis());

    public ClosestConfigurationFixer(){
        generatePrebuilts();
    }

    private void generatePrebuilts() {
        mPrebuiltArrays = new int[PREBUILT_MAX][];
        for (int i = 0; i < PREBUILT_MAX; i++) {
            mPrebuiltArrays[i] = new int[i + 1];
            for (int j = 0; j < i + 1; j++) {
                mPrebuiltArrays[i][j] = j;
            }
        }
    }


    public boolean fix(Configuration configuration, Configuration fixedConfiguration, List<Integer> includedFeatures, List<Integer> excludedFeatures) {
        Boolean res=false;

        mFeatureModel = configuration.getFeatureModel();
        fixedConfiguration.clear();
        mExcludedFeatures = new IntArrayList();
        mIncludedFeatures=new IntArrayList();

        // añadimos el conjunto de características que deben de ser incluídas de manera obligatoria.
        for(Integer inFeature:includedFeatures){
            mIncludedFeatures.add(inFeature.intValue());
        }

        // añadimos al conjunto de características excluidas aquellas que deben de excluirse según nuestra configuración.
        for(Integer exFeature:excludedFeatures){
            excludeFeature(exFeature.intValue());
        }
        // Traverse features in random order.
        // This improves the probability of traversing different paths each time.
        //Collections.shuffle(configuration.getConfigurationByID());
        return fixTreeConstraints(configuration, fixedConfiguration)
                && fixCrossTreeConstraints(configuration, fixedConfiguration);
    }

    private boolean featureCanBeIncluded(int feature, Configuration configuration) {
        int parentFeature = mFeatureModel.getParent(feature);
        return !mExcludedFeatures.contains(feature)
                && (parentFeature == 0
                || configuration.contains(parentFeature)
                || featureCanBeIncluded(parentFeature, configuration));
    }


    private Boolean excludeFeature(int feature){
        Boolean res=true;

        // No se puede excluir una característica que forma parte del conjunto de obligatorias.
        if(!mIncludedFeatures.contains(feature)) {
            mExcludedFeatures.add(feature);
            IntArrayList children = mFeatureModel.getChildren(feature);
            final int[] buffer = children.buffer;
            final int size = children.size();
            int index=0;
            while( index<size && res ) {
                res=excludeFeature(buffer[index]);
                index++;
            }
        }else{
            res=false;
        }
        return res;
    }

    private Boolean includeFeature(int feature,Configuration initialConfiguration,Configuration transformedConfiguration){
        Boolean res=true;

        if (!transformedConfiguration.contains(feature)
                && featureCanBeIncluded(feature, transformedConfiguration)) {
            // Include the feature in the configuration
            transformedConfiguration.add(feature);

            // If the feature is in an XOR group, exclude the rest of the members
            // (traversing also their branches)
            IntOpenHashSet xorMembers = mFeatureModel.getXORGroupMembers(feature);
            final int[] keys = xorMembers.keys;
            final boolean[] allocated = xorMembers.allocated;
            int index=0;
            while( index<allocated.length && res) {
                //for (int i = 0; i < allocated.length; i++) {
                if (allocated[index] && keys[index] != feature) {
                    res=excludeFeature(keys[index]);
                }
                index++;
                //}
            }
            if(res) {
                // Include the parent
                int parent = mFeatureModel.getParent(feature);
                if (parent != 0 && !transformedConfiguration.contains(parent)) {
                    res = includeFeature(parent, initialConfiguration, transformedConfiguration);
                }
                // Si se ha podido incluir el padre y sus ancestros con éxito continuamos el proceso de fix
                if(res){
                    IntArrayList children = mFeatureModel.getChildren(feature);
                    if(children.size()>0){
                        final int[] buffer = children.buffer;
                        final int size = children.size();
                        int[] iterationOrder = getRandomIterationOrder(size);
                        for (int i = 0; i < size; i++) {
                            int i_ = iterationOrder[i];
                            IntOpenHashSet groupMembers;
                            if (mFeatureModel.getORGroupMembers(buffer[i_]).size() > 0) {
                                groupMembers = mFeatureModel.getORGroupMembers(buffer[i_]);
                            } else {
                                groupMembers = mFeatureModel.getXORGroupMembers(buffer[i_]);
                            }
                            // Si la característica está incluida en un grupo y no hay ningún elemento
                            // de ese grupo en la configuración
                            if (groupMembers.size() > 0
                                    && transformedConfiguration.disjoint(groupMembers)) {
                                // The child is in a group and it is not included.
                                // We need to assure that, at least, one group's feature
                                // is added to the configuration.
                                // We have to select a feature in the group.
                                // We try to add one in the initial configuration to minimize
                                // the number of steps.
                                final int[] groupKeys = groupMembers.keys;
                                final boolean[] groupAllocated = groupMembers.allocated;
                                boolean included = false;
                                int j = 0;
                                do {
                                    if (groupAllocated[j]
                                            && initialConfiguration.contains(groupKeys[j])) {
                                        included = true;
                                        includeFeature(groupKeys[j], initialConfiguration,
                                                transformedConfiguration);
                                    }
                                    j++;
                                } while (!included && j < groupAllocated.length);
                                if (!included) {
                                    res=includeFeature(buffer[i_], initialConfiguration,
                                            transformedConfiguration);
                                }
                            } else if (mFeatureModel.isMandatory(buffer[i_])) {
                                res=includeFeature(buffer[i_], initialConfiguration,
                                        transformedConfiguration);
                            }
                        }
                    }
                }
            }
        }else{
            res=false;
        }
        return res;
    }

    private int[] getRandomIterationOrder(int size) {
        if (size <= PREBUILT_MAX) {
            shuffleArray(mPrebuiltArrays[size - 1]);
            return mPrebuiltArrays[size - 1];
        } else {
            int[] order = new int[size];
            for (int i = 0; i < size; i++) {
                order[i] = i;
            }
            shuffleArray(order);
            return order;
        }
    }

    // Modern Fisher�Yates shuffle
    private void shuffleArray(int[] ar) {

        for (int i = ar.length - 1; i > 0; i--) {
            int index = mRandom.nextInt(i + 1);
            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    // fixedConfiguration empieza incialmente vacía.
    private Boolean fixTreeConstraints(Configuration configuration,Configuration fixedConfiguration){
        Boolean res=true;

        // 1. Añadimos la raíz.
        res=includeFeature(mFeatureModel.getRootFeature(),configuration,fixedConfiguration);
        // 2. Se añaden las características que se tienen que incluir según el plan
        IntArrayList features=mIncludedFeatures;//configuration.getConfigurationByID(); // se obtiene la lista de los ids de la configuración.
        int size=features.size();
        // se intentan añadir cada uno de los elementos de la lista.
        int index=0;
        int[] buffer=features.buffer;
        while(index < size && res){
            if(!fixedConfiguration.contains(buffer[index])){
                res=includeFeature(buffer[index],configuration,fixedConfiguration);
            }
            index++;
        }
        // 3. Se eliminan las que se tienen que eliminar según el plan.
        features=mExcludedFeatures;
        size=features.size();
        index=0;
        buffer=features.buffer;
        while(index < size && res){
            if(fixedConfiguration.contains(buffer[index])){
                res=excludeFeature(buffer[index]);
            }
            index++;
        }
        // 4. Se intentan añadir la mayor cantidad posible de características de la configuración actual.
        features=configuration.getConfigurationByID();
        size=features.size();
        index=0;
        buffer=features.buffer;
        while(index < size){
            includeFeature(buffer[index],configuration,fixedConfiguration);
            index++;
        }

        // If the initial configuration is empty, we have to add the root to trigger
        // the transformation process
        /*IntArrayList features = configuration.getConfigurationByID();
        final int size = features.size();
        if (size > 0) {
            final int[] buffer = features.buffer;
            int i=0;
            while (i<size && res) {
                if (!fixedConfiguration.contains(buffer[i])) {
                    res=includeFeature(buffer[i], configuration, fixedConfiguration);
                }
                i++;
            }
        } else {
            res=includeFeature(mFeatureModel.getRootFeature(),
                    configuration, fixedConfiguration);
        }*/
        return res;
    }

    private boolean fixCrossTreeConstraints(Configuration initialConfiguration,
                                            Configuration fixedConfiguration) {
        Log.d("Self","FixCrossTreeConstraints empieza");
        List<CrossTreeConstraint> ctcs = mFeatureModel.getCrossTreeConstraints();

        int index = 0;
        int size = ctcs.size();
        if (size == 0) {
            return true;
        }
        int[] order = getRandomIterationOrder(size);
        boolean fixable = true;

        while (index < size && fixable) {
            CrossTreeConstraint ctc = ctcs.get(order[index]);
            if (!ctc.isSatisfied(fixedConfiguration)) {
                IntArrayList positive = ctc.getPositiveFeatures();

                final int[] buffer = positive.buffer;
                int ctcSize = positive.size();
                if (ctcSize > 0) {
                    int[] iterationOrder = getRandomIterationOrder(ctcSize);
                    int i = 0;
                    boolean fixed = false;

                    while (!fixed && i < ctcSize) {
                        int i_ = iterationOrder[i];
                        if (featureCanBeIncluded(buffer[i_], fixedConfiguration)) {
                            includeFeature(buffer[i_], initialConfiguration, fixedConfiguration);
                            fixed = true;
                        } else {
                            i++;
                        }
                    }
                    fixable = fixed;
                } else {
                    fixable = false;
                }
                index = 0; // We may have affected previous CTCs
            } else {
                index++;
            }
        }
        Log.d("Self","FixTreeConstraints termina");
        return fixable;
    }


}
