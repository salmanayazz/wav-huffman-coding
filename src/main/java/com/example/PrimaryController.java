package com.example;

import javafx.fxml.FXML;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.BitSet;

public class PrimaryController {
    @FXML
    private Text entropy;
    @FXML
    private Text avg_code_length;
    @FXML
    private Text file_name;

    private int[] samples;
    // <sample, count>
    Map<Integer, Integer> samplesTree;

    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a .wav File");

        // filter to only allow .wav files
        ExtensionFilter wavFilter = new ExtensionFilter("WAV Files", "*.wav");
        fileChooser.getExtensionFilters().add(wavFilter);

        // show file selection dialog
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            System.out.println("Selected .wav file: " + file.getAbsolutePath());
            file_name.setText(file.getName());
            parseFile(file);
        } else {
            System.out.println("No file selected.");
        }
    }

    private void parseFile(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            int channels = format.getChannels();
            int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
            int bytesPerFrame = sampleSizeInBytes * channels;

            // read the audio data into a byte array
            byte[] audioData = new byte[(int) (audioInputStream.getFrameLength() * bytesPerFrame)];
            audioInputStream.read(audioData);

            // extract samples into array
            samples = new int[audioData.length / sampleSizeInBytes];

            for (int i = 0, sampleIndex = 0; i < audioData.length; i += sampleSizeInBytes, sampleIndex++) {
                if (sampleSizeInBytes == 1) { // special case when sample size is 8 bits
                    // bitwise AND to set the byte to an unsigned value, then recenter with "- 128"
                    samples[sampleIndex] = (audioData[i] & 0xFF) - 128;
                } else {
                    // initialize the sample
                    samples[sampleIndex] = 0;
                    // shift the bytes and merge them while iterating over each byte in the sample
                    for (int j = 0; j < sampleSizeInBytes; j++) {
                        samples[sampleIndex] |= (audioData[i + j] & 0xFF) << (8 * j);
                    }
                }
            }

            samplesSum();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * obtains how often each sample occurs samples array then
     * calls calculateEntropy()
     */
    private void samplesSum() {
        samplesTree = new TreeMap<>();

        // place into a binary search tree to obtain the sum of the sample
        for (int i = 0; i < samples.length; i++) {
            Integer samplesSum = samplesTree.get(samples[i]);

            // if sample already exists, increment count, otherwise add
            if (samplesSum == null) {
                samplesTree.put(samples[i], 1);
            } else {
                samplesTree.put(samples[i], (samplesSum + 1));
            }
        }

        calulateEntropy();
    }

    /**
     * calculates the entropy from the samplesTree variable
     * then calls calculateAverageCodeLength()
     */
    private void calulateEntropy() {
        double entropyCalc = 0.0;

        for (Map.Entry<Integer, Integer> sampleSum : samplesTree.entrySet()) {
            double probability = (double)sampleSum.getValue() / (double)samples.length;
            entropyCalc += - (probability * (Math.log(probability) / Math.log(2)));
        }

        entropy.setText(entropyCalc + "");

        calculateAverageCodeLength();
    }

    /**
     * a node for the huffman tree
     */
    class SampleNode {
        SampleNode left;
        SampleNode right;
        Integer sum;
        Integer sample = null;
        BitSet code = null;
    }

    /**
     * performs  huffman coding on the samples to obtain the average code length
     */
    private void calculateAverageCodeLength() {
        // convert the samplesTree into an array of SampleNodes
        SampleNode[] sampleNodes = samplesTree.entrySet().stream()
            .map(entry -> {
                SampleNode sampleNode = new SampleNode();
                sampleNode.sample = entry.getKey();
                sampleNode.sum = entry.getValue();
                return sampleNode;
            }).toArray(SampleNode[]::new);
    
        // sort the SampleNodes in ascending order of sum value
        Arrays.sort(sampleNodes, (o1, o2) -> o1.sum.compareTo(o2.sum));
    
        // build the huffman tree
        while (sampleNodes.length > 1) {
            SampleNode left = sampleNodes[0];
            SampleNode right = sampleNodes[1];
    
            // create a new parent node
            SampleNode newParent = new SampleNode();
            newParent.left = left;
            newParent.right = right;
            newParent.sum = left.sum + right.sum;
    
            // replace lowest sum nodes with new parent
            sampleNodes = Arrays.copyOfRange(sampleNodes, 2, sampleNodes.length + 1);
            sampleNodes[sampleNodes.length - 1] = newParent;
    
            // resort to keep it in ascending order
            Arrays.sort(sampleNodes, (o1, o2) -> o1.sum.compareTo(o2.sum));
        }
    
        SampleNode rootNode = sampleNodes[0];
    
        double avgCodeLength = calculateAverageCodeLengthHelper(rootNode, 0);
        avg_code_length.setText(avgCodeLength + "");
    }

    /**
     * recursively calculates the average code length
     * @param node
     * the current node
     * @param depth
     * the current depth
     * @return
     * the average code length
     */
    private double calculateAverageCodeLengthHelper(SampleNode node, int depth) {
        if (node == null) {
            return 0;
        }
    
        if (node.sample != null) {
            // leaf, return calc for this node
            return (double)depth * ((double)node.sum / (double)samples.length);
        }
    
        // recurse left and right
        double left = calculateAverageCodeLengthHelper(node.left, depth + 1);
        double right = calculateAverageCodeLengthHelper(node.right, depth + 1);
    
        return left + right;
    }
}