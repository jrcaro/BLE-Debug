package com.example.juan.tanit.dspl;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntOpenHashSet;

import java.util.Arrays;
import java.util.List;

public class Configuration {

    private boolean[] mConfiguration;
    private IntArrayList mConfigurationByID;
    private FeatureModel mFeatureModel;

    public Configuration(FeatureModel fm, boolean[] configuration) {
        mFeatureModel = fm;
        mConfiguration = new boolean[configuration.length]; // Copy, avoid sharing references
        System.arraycopy(configuration, 0, mConfiguration, 0, configuration.length);
        mConfigurationByID = _convertConfiguration();
    }

    public Configuration(FeatureModel fm) {
        mFeatureModel = fm;
        mConfiguration = new boolean[fm.size()];
        mConfigurationByID = new IntArrayList();
    }

    public Configuration(Configuration config) {
        this(config.getFeatureModel(), config.getConfiguration());
    }

    public void copy(Configuration config) {
        mFeatureModel = config.getFeatureModel();
        boolean[] configuration = config.getConfiguration();
        mConfiguration = new boolean[configuration.length]; // Copy, avoid sharing references
        System.arraycopy(configuration, 0, mConfiguration, 0, configuration.length);
        mConfigurationByID = _convertConfiguration();
    }

    public Configuration getClone(){
        Configuration res=new Configuration(getFeatureModel());
        res.clear();
        for(int i=0;i<mConfigurationByID.size();i++){
            res.add(mConfigurationByID.get(i));
        }
        return res;
    }



    public FeatureModel getFeatureModel() {
        return mFeatureModel;
    }

    public int getIndexOfFeature(int featureId){
        int res=0;
        IntArrayList featureList=mFeatureModel.getFeatures();
        int index=0;
        Boolean success=false;
        while(index<featureList.size() && !success){
            if(featureList.get(index)==featureId){
                success=true;
                res=index;
            }
            index++;
        }
        return res;
    }

    public void set(int index, boolean value) {
        if (mConfiguration != null && index < mConfiguration.length) {
            mConfiguration[index] = value;
            int id = mFeatureModel.getFeatures().get(index);
            if (value) {
                mConfigurationByID.add(id);
            } else {
                mConfigurationByID.remove(mConfigurationByID.indexOf(id));
            }
        }
    }

    public void add(int id) {
        int index = mFeatureModel.getFeatures().indexOf(id);
        if (index != -1 && !mConfiguration[index]) {
            mConfiguration[index] = true;
            mConfigurationByID.add(id);
        }
    }

    public void remove(int id) {
        int index = mFeatureModel.getFeatures().indexOf(id);
        if (index != -1 && !mConfiguration[index]) {
            mConfiguration[index] = false;
            //mConfigurationByID.add(id);
            int internalIndex=0;
            Boolean success=false;
            while(!success && internalIndex<mConfigurationByID.size()){
                if(mConfigurationByID.get(internalIndex)==id){
                    success=true;
                    mConfigurationByID.remove(internalIndex);
                }
                internalIndex++;
            }
        }
    }

    public void clear() {
        Arrays.fill(mConfiguration, false);
        mConfigurationByID.clear();
    }

    public boolean disjoint(IntOpenHashSet features) {
        final int[] keys = features.keys;
        final boolean[] allocated = features.allocated;

        boolean disjoint = true;
        int i = 0;
        while (disjoint && i < allocated.length) {
            if (allocated[i]) {
                disjoint = !mConfigurationByID.contains(keys[i]);
            }
            i++;
        }
        return disjoint;
    }

    public boolean[] getConfiguration() {
        return mConfiguration;
    }

    public IntArrayList getConfigurationByID() {
        return mConfigurationByID;
    }

    public boolean contains(int id) {
        return mConfigurationByID.contains(id);
    }

    public int size() {
        return mConfiguration.length;
    }

    public int diff(Configuration configuration2) {
        int diff = 0;
        if (configuration2.size() != mConfiguration.length) {
            diff = -1;
        } else {
            boolean[] config2 = configuration2.getConfiguration();
            for (int i = 0; i < mConfiguration.length; i++) {
                if (mConfiguration[i] != config2[i]) {
                    diff++;
                }
            }
        }
        return diff;
    }

    /* Methods for managing data representation */
    private IntArrayList _convertConfiguration() {
        IntArrayList configuration = new IntArrayList();
        IntArrayList features = mFeatureModel.getFeatures();
        for (int i = 0; i < mConfiguration.length; i++) {
            if (mConfiguration[i]) {
                configuration.add(features.get(i));
            }
        }
        return configuration;
    }

    /* Methods for calculating optimization data */
    public int getResourceUsage() {
        int resourceUsage = 0;

        final int[] buffer = mConfigurationByID.buffer;
        final int size = mConfigurationByID.size();

        for (int i = 0; i < size; i++) {
            resourceUsage += mFeatureModel.getResourceUsage(buffer[i]);
        }

        return resourceUsage;
    }

    public int getUtility() {
        int utility = 0;

        final int[] buffer = mConfigurationByID.buffer;
        final int size = mConfigurationByID.size();

        for (int i = 0; i < size; i++) {
            utility += mFeatureModel.getUtility(buffer[i]);
        }

        return utility;
    }

    public String toString() {
        StringBuilder output = new StringBuilder();

        final int[] buffer = mConfigurationByID.buffer;
        final int size = mConfigurationByID.size();

        for (int i = 0; i < size; i++) {
            output.append(mFeatureModel.getName(buffer[i]));
            output.append(" ");
        }

        // Delete last blank space
        if (output.length() > 0) {
            output.deleteCharAt(output.length() - 1);
        }

        return output.toString();
    }


    public int compareTo(Configuration o) {
        int myUtility = getUtility();
        int oUtility = o.getUtility();
        int result = 0;

        if (myUtility < oUtility) {
            result = -1;
        } else if (myUtility > oUtility) {
            result = 1;
        }

        return result;
    }

    public boolean checkTreeConstraints() {
        int rootFeature = mFeatureModel.getRootFeature();

        IntOpenHashSet checkedFeatures = new IntOpenHashSet();
        return mConfigurationByID.contains(rootFeature)
                && checkFeature(rootFeature, checkedFeatures)
                && checkedFeatures.size() == mConfigurationByID.size();
    }

    // Top-down checking
    private boolean checkFeature(int feature, IntOpenHashSet checkedFeatures) {
        checkedFeatures.add(feature);
        IntArrayList children = mFeatureModel.getChildren(feature);

        // Check parent-child constraints
        final int[] buffer = children.buffer;
        final int size = children.size();
        for (int i = 0; i < size; i++) {
            int c = buffer[i];

            if (mFeatureModel.isMandatory(c) && !mConfigurationByID.contains(c)) {
                return false;
            }

            IntOpenHashSet xorMembers = mFeatureModel.getXORGroupMembers(c);
            if (xorMembers.size() > 0) {
                int included = 0;
                final int[] groupKeys = xorMembers.keys;
                final boolean[] groupAllocated = xorMembers.allocated;
                for (int j = 0; j < groupAllocated.length; j++) {
                    if (groupAllocated[j] && mConfigurationByID.contains(groupKeys[j])) {
                        included++;
                    }
                }
                if (included != 1) {
                    return false;
                }
            }

            IntOpenHashSet orMembers = mFeatureModel.getORGroupMembers(c);
            if (orMembers.size() > 0) {
                boolean included = false;
                final int[] groupKeys = orMembers.keys;
                final boolean[] groupAllocated = orMembers.allocated;
                int j = 0;
                while (!included && j < groupAllocated.length) {
                    included = groupAllocated[j]
                            && mConfigurationByID.contains(groupKeys[j]);
                    j++;
                }

                if (!included) {
                    return false;
                }
            }
        }

        // Check each included child
        for (int i = 0; i < size; i++) {
            if (mConfigurationByID.contains(buffer[i])
                    && !checkFeature(buffer[i], checkedFeatures)) {
                return false;
            }
        }

        return true;
    }

    public boolean checkCrossTreeConstraints() {
        boolean satisfied = true;

        List<CrossTreeConstraint> ctcs = mFeatureModel.getCrossTreeConstraints();

        final int size = ctcs.size();
        int i = 0;

        while (satisfied && i < size) {
            satisfied = ctcs.get(i).isSatisfied(this);
            i++;
        }

        return satisfied;
    }

    @Override
    public boolean equals(Object o) {
        Configuration that = (Configuration) o;
        IntArrayList thatList=that.getConfigurationByID();
        Boolean res=true;

        if(this.getConfigurationByID().size()==thatList.size()){
            int index=0;
            while(index<thatList.size() && res){
                if(!this.getConfigurationByID().contains(thatList.get(index))){
                    res=false;
                }
                index++;
            }
        }else{
            res=false;
        }

        return res;
    }

    public Configuration getDifferences(Configuration conf){
        Configuration res=null;

        boolean[] thisConfArray=getConfiguration();
        boolean[] resArray=new boolean[thisConfArray.length];
        boolean[] newConfArray=conf.getConfiguration();
        if(thisConfArray.length==newConfArray.length) {
            for (int i = 0; i < thisConfArray.length; i++) {
                resArray[i]=thisConfArray[i]^newConfArray[i];
            }
            res=new Configuration(mFeatureModel,resArray);
        }
        return res;
    }
}
