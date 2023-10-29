package com.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.File;

public class PrimaryController {
    @FXML
    private Text entropy;
    @FXML
    private Text avg_code_length;
    @FXML
    private Text file_name;

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
            int[] samples = new int[audioData.length / sampleSizeInBytes];

            for (int i = 0; i < audioData.length; i += sampleSizeInBytes) {
                if (sampleSizeInBytes == 1) { // special case when sample size is 8 bits
                    // bitwise AND to set the bytes to an unsigned value, then recenter with "- 128"
                    samples[i / sampleSizeInBytes] = (audioData[i] & 0xFF) - 128;
                } else {
                    // shift the MSB left to merge it with the LSB while iterating over each byte in the sample
                    for (int j = 0; j < sampleSizeInBytes; j++) {
                        samples[i / sampleSizeInBytes] = (samples[i / sampleSizeInBytes] | (audioData[i + j] << 8 * j));
                    }
                }
            }

            printSamples(samples);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printSamples(int[] samples) {
        for (int i = 0; i < samples.length; i++) {
            System.out.println("index " +  i + ": " + samples[i]);
        }
    }

}