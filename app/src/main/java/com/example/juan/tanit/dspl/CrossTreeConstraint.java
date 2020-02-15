package com.example.juan.tanit.dspl;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.carrotsearch.hppc.IntArrayList;

import java.util.Objects;

public class CrossTreeConstraint {
    /**
     * Length of the positive an negative features. It's just an slight
     * optimization.
     */
    private int mPositiveFeaturesSize, mNegativeFeaturesSize;

    /**
     * Positive and negative features. Stored in arrays and lists in order to
     * improve access performance
     */
    private IntArrayList mPositiveFeatures, mNegativeFeatures;

    // Copy constructor to prevent external modifications of mutable properties
    public CrossTreeConstraint(CrossTreeConstraint c) {
        mPositiveFeaturesSize = c.mPositiveFeaturesSize;
        mNegativeFeaturesSize = c.mNegativeFeaturesSize;
        mPositiveFeatures = c.mPositiveFeatures.clone();
        mNegativeFeatures = c.mNegativeFeatures.clone();
    }

    /**
     * Generates the constraint straight from the lists of features
     *
     * @param positiveFeatures
     * @param negativeFeatures
     */
    public CrossTreeConstraint(IntArrayList positiveFeatures, IntArrayList negativeFeatures) {
        mPositiveFeatures = positiveFeatures.clone();
        mNegativeFeatures = negativeFeatures.clone();
        mPositiveFeaturesSize = mPositiveFeatures.size();
        mNegativeFeaturesSize = mNegativeFeatures.size();
    }



    /**
     * Checks whether a configuration satisfies this constraints
     *
     * @param configuration the configuration
     * @return true if the constraint is satisfied
     */
    public boolean isSatisfied(Configuration configuration) {
        // If one feature of the configuration is contained in the positive features, the constraint is satisfied.

        int[] buffer = mPositiveFeatures.buffer;
        for (int i = 0; i < mPositiveFeaturesSize; i++) {
            if (configuration.contains(buffer[i])) {
                return true;
            }
        }

        // If it is not satisfied, we will check if any of the negative features is not found in configuration.
        buffer = mNegativeFeatures.buffer;
        for (int i = 0; i < mNegativeFeaturesSize; i++) {
            if (!configuration.contains(buffer[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the positive features of the constraint. If one of these features
     * is included in the configuration, then the constraint is satisfied.
     *
     * @return the positive features
     */
    public IntArrayList getPositiveFeatures() {
        return mPositiveFeatures;
    }

    /**
     * Returns the negative features of the constraint. If at least one of these
     * features is not included in the configuration, then the constraint is
     * satisfied.
     *
     * @return the negative features
     */
    public IntArrayList getNegativeFeatures() {
        return mNegativeFeatures;
    }

    public void addPositiveFeature(int feature){
        mPositiveFeatures.add(feature);
        mPositiveFeaturesSize++;
    }

    public void addNegativeFeature(int feature){
        mNegativeFeatures.add(feature);
        mNegativeFeaturesSize++;
    }

    public String toString() {
        return "+ " + mPositiveFeatures.toString() + " ; - " + mNegativeFeatures.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrossTreeConstraint that = (CrossTreeConstraint) o;
        return mPositiveFeaturesSize == that.mPositiveFeaturesSize &&
                mNegativeFeaturesSize == that.mNegativeFeaturesSize &&
                Objects.equals(mPositiveFeatures, that.mPositiveFeatures) &&
                Objects.equals(mNegativeFeatures, that.mNegativeFeatures);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(mPositiveFeaturesSize, mNegativeFeaturesSize, mPositiveFeatures, mNegativeFeatures);
    }
}
