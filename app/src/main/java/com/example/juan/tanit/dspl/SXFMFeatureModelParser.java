package com.example.juan.tanit.dspl;

import com.carrotsearch.hppc.IntArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SXFMFeatureModelParser {
    /**
     * Parses an FM stored in a file in SXFM syntax. Optionally, optimization
     * data (resource usage and utility) can be parsed.
     *
     * @param featureModelFile the SXFM file
     * @param optimizationDataFile the optimization data file
     * @return the feature model
     * @throws java.io.IOException
     */
    public static FeatureModel parse(String featureModelFile, String optimizationDataFile)
            throws IOException {

        FeatureModel fm = new FeatureModel();
        String featureModelID = new File(featureModelFile).getName();
        featureModelID = featureModelID.substring(0, featureModelID.length() - 4);
        fm.setID(featureModelID);

        // Open files
        BufferedReader fmReader = new BufferedReader(new FileReader(featureModelFile));
        BufferedReader optReader = new BufferedReader(new FileReader(optimizationDataFile));

        // Go to the beginning of the feature tree
        String line;
        do {
            line = fmReader.readLine();
        } while (line != null && !line.startsWith("<feature_tree>"));

        // Retrieve all the feature tree information
        List<String> featureTreeLines = new ArrayList<String>();
        line = fmReader.readLine();
        while (line != null && !line.startsWith("</feature_tree>")) {
            featureTreeLines.add(line);
            line = fmReader.readLine();
        }

        // Start recursive parsing process in the first line
        parseLine(featureTreeLines.toArray(
                new String[featureTreeLines.size()]),
                0, fm, 0);

        featureTreeLines.clear(); // Clear list

        // Go to the beginning of the cross-tree constraints
        do {
            line = fmReader.readLine();
        } while (line != null && !line.startsWith("<constraints>"));

        // Check whether there are cross-tree constraints
        if (line != null) {
            line = fmReader.readLine();
            while (line != null && !line.startsWith("</constraints>")) {
                parseCrossTreeConstraint(line, fm);
                line = fmReader.readLine();
            }
        }

        // Feature model parsing is finished
        fmReader.close();

        // Parse optimization data
        if (optimizationDataFile != null) {
            while ((line = optReader.readLine()) != null) {
                String[] data = line.split(" ");
                int id = fm.getID(data[0].trim().toLowerCase());
                Double aux=Double.parseDouble(data[1]);
                int resourceUsage = aux.intValue();
                aux=Double.parseDouble(data[2]);
                int utility =aux.intValue();
                fm.addOptimizationData(id, resourceUsage, utility);
            }
        }

        optReader.close();

        // Feature model and optimization data parsing finished
        return fm;
    }

    public static FeatureModel parse(String featureModelFile)
            throws IOException {

        FeatureModel fm = new FeatureModel();
        String featureModelID = new File(featureModelFile).getName();
        featureModelID = featureModelID.substring(0, featureModelID.length() - 4);
        fm.setID(featureModelID);

        // Open files
        BufferedReader fmReader = new BufferedReader(new FileReader(featureModelFile));
        //BufferedReader optReader = new BufferedReader(new FileReader(optimizationDataFile));

        // Go to the beginning of the feature tree
        String line;
        do {
            line = fmReader.readLine();
        } while (line != null && !line.startsWith("<feature_tree>"));

        // Retrieve all the feature tree information
        List<String> featureTreeLines = new ArrayList<String>();
        line = fmReader.readLine();
        while (line != null && !line.startsWith("</feature_tree>")) {
            featureTreeLines.add(line);
            line = fmReader.readLine();
        }

        // Start recursive parsing process in the first line
        parseLine(featureTreeLines.toArray(
                new String[featureTreeLines.size()]),
                0, fm, 0);

        featureTreeLines.clear(); // Clear list

        // Go to the beginning of the cross-tree constraints
        do {
            line = fmReader.readLine();
        } while (line != null && !line.startsWith("<constraints>"));

        // Check whether there are cross-tree constraints
        if (line != null) {
            line = fmReader.readLine();
            while (line != null && !line.startsWith("</constraints>")) {
                parseCrossTreeConstraint(line, fm);
                line = fmReader.readLine();
            }
        }

        // Feature model parsing is finished
        fmReader.close();

        IntArrayList featureList=fm.getFeatures();
        // se generan valores de optimización aleatorios para cada característica.
        //Random seed=new Random();

        for(int i=0;i<featureList.size();i++){
            int resourceUsage=(int) (Math.random() * 100) + 1;
            int utility=(int) (Math.random() * 100) + 1;
            fm.addOptimizationData(featureList.get(i), resourceUsage, utility);
        }
        // Parse optimization data
        /*if (optimizationDataFile != null) {
            while ((line = optReader.readLine()) != null) {
                String[] data = line.split(" ");
                int id = fm.getID(data[0].trim().toLowerCase());
                int resourceUsage = Integer.parseInt(data[1]);
                int utility = Integer.parseInt(data[2]);
                fm.addOptimizationData(id, resourceUsage, utility);
            }
        }

        optReader.close();*/

        // Feature model and optimization data parsing finished
        return fm;
    }

    /**
     * Parses a line in the feature model tree
     *
     * @param tree feature model tree
     * @param lineIndex index of the line to parse
     * @param fm feature model
     * @param parentFeature the ID of the parent of the feature specified in
     * this line
     * @return the next line that should be parsed
     * @throws java.io.IOException
     */
    private static int parseLine(String[] tree, int lineIndex, FeatureModel fm, int parentFeature) throws IOException {

        // The fist feature should be the root feature
        if (parentFeature == 0 && !firstWord(tree[lineIndex]).equals(":r")) {
            return -1;
        } else if (parentFeature == 0) {
            int id = fm.addFeature(getName(tree[lineIndex]), 0, true);
            return parseLine(tree, 1, fm, id);
        } else {
            int currentDepth = getDepth(tree[lineIndex]);
            do {
                // Process children in this depth.
                // Higher depth is processed and then we return to process our depth until the depth is lower.
                String type = firstWord(tree[lineIndex]);
                if (type.equals(":m") || type.equals(":o")) {
                    // Mandatory or optional feature
                    boolean mandatory = type.equals(":m");
                    int id = fm.addFeature(getName(tree[lineIndex]), parentFeature, mandatory);
                    if (lineIndex + 1 < tree.length
                            && getDepth(tree[lineIndex + 1]) > currentDepth) {
                        lineIndex = parseLine(tree, lineIndex + 1, fm, id);
                    } else {
                        lineIndex++;
                    }
                } else if (type.equals(":g")) {
                    // Group of features
                    // Only OR or XOR groups supported
                    // Lower bound is supposed to be 1
                    int groupType = getUpperBound(tree[lineIndex]) == -1
                            ? FeatureModel.TYPE_OR : FeatureModel.TYPE_XOR;

                    Collection<String> members = new ArrayList<String>();
                    int groupDepth = currentDepth + 1;
                    lineIndex++;

                    // Get the list of features of the group
                    int fastGroupParseIndex = lineIndex;
                    do {
                        members.add(getName(tree[fastGroupParseIndex++]));
                        while (fastGroupParseIndex < tree.length
                                && getDepth(tree[fastGroupParseIndex]) > groupDepth) {
                            fastGroupParseIndex++;
                        }
                    } while (fastGroupParseIndex < tree.length && getDepth(tree[fastGroupParseIndex]) == groupDepth);

                    // Create the group of features
                    String[] names = members.toArray(new String[members.size()]);
                    int[] ids = fm.addFeatureGroup(names, parentFeature, groupType);

                    // Parse the features in the group
                    int featureIndex = 0;
                    do {
                        if ((lineIndex + 1) < tree.length && getDepth(tree[lineIndex + 1]) > groupDepth) {
                            lineIndex = parseLine(tree, lineIndex + 1, fm, ids[featureIndex]);
                        } else {
                            lineIndex++;
                        }
                        featureIndex++;
                    } while (lineIndex < tree.length && getDepth(tree[lineIndex]) > currentDepth);
                } else {
                    // Unexpected line, ignore it
                    lineIndex++;
                }
            } while (lineIndex < tree.length && getDepth(tree[lineIndex]) == currentDepth);

            return lineIndex;
        }
    }

    /**
     * Parses a line specifying a cross-tree constraint
     *
     * @param line the line containing the cross-tree constraint
     * @param fm the feature model
     */
    private static void parseCrossTreeConstraint(String line, FeatureModel fm) {
        line = line.toLowerCase();
        int start = line.indexOf(':');
        int index = start + 1;
        IntArrayList positive = new IntArrayList();
        IntArrayList negative = new IntArrayList();
        while (index < line.length()) {
            int featureEnd = line.indexOf(' ', index);
            if (featureEnd == -1) {
                featureEnd = line.length();
            }
            if (line.charAt(index) == '~') {
                String name = line.substring(index + 1, featureEnd);
                negative.add(fm.getID(name));
            } else {
                String name = line.substring(index, featureEnd);
                positive.add(fm.getID(name));
            }
            index = line.indexOf("or", featureEnd + 1);
            index = index == -1 ? line.length() : index + 3; // Skip OR connector
        }
        fm.addCrossTreeConstraint(positive, negative);
    }

    // This method is not currently required
    /*
	private static int getLowerBound(String line) {
		int start = line.indexOf('[');
		int end = line.indexOf(',', start);
		return Integer.parseInt(line.substring(start + 1, end));
	}
     */
    /**
     * Gets the upper bound of a feature group encoded in this line
     *
     * @param line the line where the feature group is encoded
     * @return the upper bound of the feature group
     */
    private static int getUpperBound(String line) {
        int end = line.indexOf('[');
        int start = line.indexOf(',', end);
        end = line.indexOf(']', start);
        String upperBound = line.substring(start + 1, end);
        return upperBound.equals("*") ? -1 : Integer.parseInt(upperBound);
    }

    /**
     * Gets the depth (in number of tabulations) of the line
     *
     * @param line the line
     * @return the depth
     */
    private static int getDepth(String line) {
        int depth = 0;
        while (line.charAt(depth++) == '\t');
        return depth;
    }

    /**
     * Gets the first word in a line
     *
     * @param line the line
     * @return the first word
     */
    private static String firstWord(String line) {
        int start = 0;
        while (line.charAt(start) == '\t') {
            start++;
        }
        int end = line.indexOf(' ', start);
        return line.substring(start, end);
    }

    /**
     * Extracts the name of a feature in the feature tree
     *
     * @param line the line encoding the feature
     * @return the name of the feature
     */
    private static String getName(String line) {
        int start = line.indexOf('(');
        int end = line.indexOf(')');
        return line.substring(start + 1, end);
    }

}
