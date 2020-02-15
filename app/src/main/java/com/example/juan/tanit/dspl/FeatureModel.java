package com.example.juan.tanit.dspl;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FeatureModel {
    public static final int TYPE_OR = 1;
    public static final int TYPE_XOR = 2;

    /**
     * ID of the feature model
     */
    private String mID = "";

    /**
     * List of feature names
     */
    private IntObjectOpenHashMap<String> mFeatureNames;

    /**
     * Inverted list of feature names
     */
    private ObjectIntOpenHashMap<String> mNamesToID;

    /**
     * List of features identifiers
     */
    private IntArrayList mFeatures;

    /**
     * The parent of each feature, 0 if it is the root feature
     */
    private IntIntOpenHashMap mParents;

    /**
     * Members of the OR group in which a feature is included (if applicable)
     */
    private IntObjectOpenHashMap<IntOpenHashSet> mORGroupMembers;

    /**
     * Members of the XOR group in which a feature is included (if applicable)
     */
    private IntObjectOpenHashMap<IntOpenHashSet> mXORGroupMembers;

    /**
     * Mandatory features
     */
    private IntOpenHashSet mMandatoryFeatures;

    /**
     * Cross-tree constraints
     */
    private List<CrossTreeConstraint> mCrossTreeConstraints;

    /**
     * Resource usage of each feature
     */
    private IntIntOpenHashMap mResourceUsage;

    /**
     * Utility of each feature
     */
    private IntIntOpenHashMap mUtility;

    private AtomicInteger mIDFactory = new AtomicInteger(1);

    /*
     * The following fields are redundant but optimize the execution time
     */
    private IntObjectOpenHashMap<IntArrayList> mChildren;

    public FeatureModel() {
        mFeatures = new IntArrayList();
        mFeatureNames = new IntObjectOpenHashMap<String>();
        mNamesToID = new ObjectIntOpenHashMap<String>();
        mParents = new IntIntOpenHashMap();
        mResourceUsage = new IntIntOpenHashMap();
        mUtility = new IntIntOpenHashMap();
        mMandatoryFeatures = new IntOpenHashSet();
        mORGroupMembers = new IntObjectOpenHashMap<IntOpenHashSet>();
        mXORGroupMembers = new IntObjectOpenHashMap<IntOpenHashSet>();
        mCrossTreeConstraints = new ArrayList<CrossTreeConstraint>();
        mChildren = new IntObjectOpenHashMap<IntArrayList>();
    }

    private int getNextID() {
        return mIDFactory.getAndIncrement();
    }

    /* Methods for modifying the feature model */
    /**
     * Adds a feature to the feature model
     *
     * @param name Name of the feature
     * @param parent ID of the parent, or 0 if it is the root feature
     * @param mandatory True if the relationship is mandatory, false if it is
     * optional
     * @return the ID of the new feature
     */
    public int addFeature(String name, int parent, boolean mandatory) {
        int id = getNextID();
        name = name.toLowerCase();
        mFeatures.add(id);
        mFeatureNames.put(id, name);
        mNamesToID.put(name, id);
        mParents.put(id, parent);
        if (parent != 0 && mandatory) {
            mMandatoryFeatures.add(id);
        }

        mChildren.put(id, new IntArrayList());
        if (parent != 0) {
            mChildren.get(parent).add(id);
        }

        mORGroupMembers.put(id, new IntOpenHashSet());
        mXORGroupMembers.put(id, new IntOpenHashSet());
        mUtility.put(id, 0);
        mResourceUsage.put(id, 0);

        return id;
    }

    public int[] addFeatureGroup(String[] names, int parent, int type) {
        int[] ids = new int[names.length];
        IntOpenHashSet groupMembers = new IntOpenHashSet();

        for (int i = 0; i < names.length; i++) {
            ids[i] = getNextID();
            String name = names[i].toLowerCase();
            groupMembers.add(ids[i]);
            mFeatures.add(ids[i]);
            mFeatureNames.put(ids[i], name);
            mNamesToID.put(name, ids[i]);
            mParents.put(ids[i], parent);
            mUtility.put(ids[i], 0);
            mResourceUsage.put(ids[i], 0);

            mChildren.put(ids[i], new IntArrayList());
            mChildren.get(parent).add(ids[i]);
        }

        for (int id : ids) {
            if (type == TYPE_OR) {
                mORGroupMembers.put(id, groupMembers);
                mXORGroupMembers.put(id, new IntOpenHashSet());
            } else {
                mORGroupMembers.put(id, new IntOpenHashSet());
                mXORGroupMembers.put(id, groupMembers);
            }
        }

        return ids;
    }

    public void addCrossTreeConstraint(IntArrayList positive, IntArrayList negative) {
        mCrossTreeConstraints.add(new CrossTreeConstraint(positive, negative));
    }

    public void addCrossTreeConstraint(CrossTreeConstraint crossTreeConstraint) {
        mCrossTreeConstraints.add(crossTreeConstraint);
    }

    public void addOptimizationData(int id, int resourceUsage, int utility) {
        mResourceUsage.put(id, resourceUsage);
        mUtility.put(id, utility);
    }

    /* Methods for retrieving information */
    public String getID() {
        return mID;
    }

    public void setID(String id) {
        mID = id;
    }

    public String getName(int id) {
        return mFeatureNames.get(id);
    }

    public IntArrayList getFeatures() {
        return mFeatures;
    }

    public int getParent(int id) {
        return mParents.get(id);
    }

    public IntOpenHashSet getORGroupMembers(int id) {
        return mORGroupMembers.get(id);
    }

    public IntOpenHashSet getXORGroupMembers(int id) {
        return mXORGroupMembers.get(id);
    }

    public IntOpenHashSet getGroupMembers(int id) {
        return mORGroupMembers.get(id).size() > 0 ? mORGroupMembers.get(id) : mXORGroupMembers.get(id);
    }

    public boolean isMandatory(int id) {
        return mMandatoryFeatures.contains(id);
    }

    public List<CrossTreeConstraint> getCrossTreeConstraints() {
        return mCrossTreeConstraints;
    }

    public int size() {
        return mFeatures.size();
    }

    public int getRootFeature() {
        int rootFeature = 0;
        boolean rootFound = false;

        final int[] keys = mParents.keys;
        final int[] values = mParents.values;
        final boolean[] allocated = mParents.allocated;
        int i = 0;
        while (!rootFound && i < allocated.length) {
            if (allocated[i] && values[i] == 0) {
                rootFound = true;
                rootFeature = keys[i];
            } else {
                i++;
            }
        }

        return rootFeature;
    }

    public Boolean containsFeature(String name){
        return mNamesToID.containsKey(name);
    }

    public int getID(String name) {
        return mNamesToID.get(name);
    }

    public IntArrayList getChildren(int id) {
        return mChildren.get(id);
    }

    public int getResourceUsage(int id) {
        return mResourceUsage.get(id);
    }

    public int getUtility(int id) {
        return mUtility.get(id);
    }






}


