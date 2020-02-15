package com.example.juan.tanit.dspl;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;

import java.util.List;

public class BasicFix {

    private FeatureModel mFeatureModel;

    private IntOpenHashSet mExcludedFeatures;

    private int[][] mPrebuiltArrays;
    private static final int PREBUILT_MAX = 50;
    private XSRandom mRandom = new XSRandom(System.currentTimeMillis());

    public BasicFix() {
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


    public boolean fix(Configuration configuration,
                       Configuration fixedConfiguration) {

        mFeatureModel = configuration.getFeatureModel();

        fixedConfiguration.clear();
        mExcludedFeatures = new IntOpenHashSet();

        // Traverse features in random order.
        // This improves the probability of traversing different paths each time.
        //Collections.shuffle(configuration.getConfigurationByID());
        return fixTreeConstraints(configuration, fixedConfiguration)
                && fixCrossTreeConstraints(configuration, fixedConfiguration);
    }

    private boolean fixCrossTreeConstraints(Configuration initialConfiguration,
                                            Configuration fixedConfiguration) {
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
        return fixable;
    }

    private boolean featureCanBeIncluded(int feature, Configuration configuration) {
        int parentFeature = mFeatureModel.getParent(feature);
        return !mExcludedFeatures.contains(feature)
                && (parentFeature == 0
                || configuration.contains(parentFeature)
                || featureCanBeIncluded(parentFeature, configuration));
    }

    /**
     * Transforms a configuration in order to satisfy the tree constraints.
     *
     * @param configuration Configuration to fix
     * @return A fixed configuration
     */
    private boolean fixTreeConstraints(Configuration configuration,
                                       Configuration fixedConfiguration) {

        // If the initial configuration is empty, we have to add the root to trigger
        // the transformation process
        IntArrayList features = configuration.getConfigurationByID();
        final int size = features.size();
        if (size > 0) {
            final int[] buffer = features.buffer;
            int i = 0;
            do {
                if (!fixedConfiguration.contains(buffer[i])) {
                    includeFeature(buffer[i], configuration, fixedConfiguration);
                }
                i++;
            } while (i < size);
        } else {
            includeFeature(mFeatureModel.getRootFeature(),
                    configuration, fixedConfiguration);
        }
        // si  hemos llegado hasta aquí es que la configuración podía arreglarse.
        return true;
    }

    private void excludeFeature(int feature) {
        mExcludedFeatures.add(feature);
        IntArrayList children = mFeatureModel.getChildren(feature);
        final int[] buffer = children.buffer;
        final int size = children.size();
        for (int i = 0; i < size; i++) {
            excludeFeature(buffer[i]);
        }
    }

    private void includeFeature(int feature,
                                Configuration initialConfiguration,
                                Configuration transformedConfiguration) {

        if (!transformedConfiguration.contains(feature)
                && featureCanBeIncluded(feature, transformedConfiguration)) {
            // Include the feature in the configuration
            transformedConfiguration.add(feature);

            // If the feature is in an XOR group, exclude the rest of the members
            // (traversing also their branches)
            IntOpenHashSet xorMembers = mFeatureModel.getXORGroupMembers(feature);
            final int[] keys = xorMembers.keys;
            final boolean[] allocated = xorMembers.allocated;
            for (int i = 0; i < allocated.length; i++) {
                if (allocated[i] && keys[i] != feature) {
                    excludeFeature(keys[i]);
                }
            }

            // Include the parent
            int parent = mFeatureModel.getParent(feature);
            if (parent != 0 && !transformedConfiguration.contains(parent)) {
                includeFeature(parent, initialConfiguration, transformedConfiguration);
            }

            //Collections.shuffle(children); // Always try different paths
            IntArrayList children = mFeatureModel.getChildren(feature);
            if (children.size() > 0) {
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
                            includeFeature(buffer[i_], initialConfiguration,
                                    transformedConfiguration);
                        }
                    } else if (mFeatureModel.isMandatory(buffer[i_])) {
                        includeFeature(buffer[i_], initialConfiguration,
                                transformedConfiguration);
                    }
                }
            }
        }
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
}
