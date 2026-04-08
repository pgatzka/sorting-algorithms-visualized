package io.github.pgatzka.videogen.encoding;

import io.github.pgatzka.videogen.algorithm.SortingState;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioGenerator {

  private static final int SAMPLE_RATE = 44100;
  private static final double MIN_FREQ = 200.0;
  private static final double MAX_FREQ = 1400.0;

  public record AudioPhase(int paddingFrames, List<SortingState> states) {}

  public void generate(
      Path outputPath,
      List<AudioPhase> phases,
      int framesPerStep,
      int fps,
      int maxValue,
      int trailingPaddingFrames)
      throws IOException {

    double secondsPerState = (double) framesPerStep / fps;
    int samplesPerState = (int) (secondsPerState * SAMPLE_RATE);

    int totalSamples = 0;
    for (AudioPhase phase : phases) {
      int paddingSamples = (int) ((double) phase.paddingFrames / fps * SAMPLE_RATE);
      totalSamples += paddingSamples + (phase.states.size() * samplesPerState);
    }
    int trailingSamples = (int) ((double) trailingPaddingFrames / fps * SAMPLE_RATE);
    totalSamples += trailingSamples;

    log.info(
        "Generating audio: {} phases, {} total samples, {}s duration",
        phases.size(),
        totalSamples,
        (double) totalSamples / SAMPLE_RATE);

    try (DataOutputStream out =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))) {
      writeWavHeader(out, totalSamples);

      for (AudioPhase phase : phases) {
        int paddingSamples = (int) ((double) phase.paddingFrames / fps * SAMPLE_RATE);
        writeSilence(out, paddingSamples);

        for (SortingState state : phase.states) {
          if (state.compareIdx1() >= 0 && state.compareIdx2() >= 0) {
            int val1 = state.array()[state.compareIdx1()];
            int val2 = state.array()[state.compareIdx2()];
            double freq1 = valueToFrequency(val1, maxValue);
            double freq2 = valueToFrequency(val2, maxValue);
            writeTone(out, (freq1 + freq2) / 2.0, samplesPerState);
          } else if (state.compareIdx1() >= 0) {
            int val = state.array()[state.compareIdx1()];
            writeTone(out, valueToFrequency(val, maxValue), samplesPerState);
          } else {
            writeSilence(out, samplesPerState);
          }
        }
      }

      writeSilence(out, trailingSamples);
    }

    log.info("Audio generated: {}", outputPath);
  }

  private double valueToFrequency(int value, int maxValue) {
    double ratio = (double) value / maxValue;
    return MIN_FREQ + ratio * (MAX_FREQ - MIN_FREQ);
  }

  private void writeTone(DataOutputStream out, double frequency, int numSamples)
      throws IOException {
    int fadeLength = Math.min(numSamples / 4, SAMPLE_RATE / 200);
    for (int i = 0; i < numSamples; i++) {
      double t = (double) i / SAMPLE_RATE;
      double amplitude = Math.sin(2.0 * Math.PI * frequency * t);

      double envelope = 0.3;
      if (i < fadeLength) {
        envelope *= (double) i / fadeLength;
      } else if (i > numSamples - fadeLength) {
        envelope *= (double) (numSamples - i) / fadeLength;
      }

      short sample = (short) (amplitude * envelope * Short.MAX_VALUE);
      out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(sample).array());
    }
  }

  private void writeSilence(DataOutputStream out, int numSamples) throws IOException {
    byte[] silence = new byte[2];
    for (int i = 0; i < numSamples; i++) {
      out.write(silence);
    }
  }

  private void writeWavHeader(DataOutputStream out, int totalSamples) throws IOException {
    int dataSize = totalSamples * 2;
    int fileSize = 36 + dataSize;

    out.writeBytes("RIFF");
    out.write(intToLittleEndian(fileSize));
    out.writeBytes("WAVE");

    out.writeBytes("fmt ");
    out.write(intToLittleEndian(16));
    out.write(shortToLittleEndian((short) 1));
    out.write(shortToLittleEndian((short) 1));
    out.write(intToLittleEndian(SAMPLE_RATE));
    out.write(intToLittleEndian(SAMPLE_RATE * 2));
    out.write(shortToLittleEndian((short) 2));
    out.write(shortToLittleEndian((short) 16));

    out.writeBytes("data");
    out.write(intToLittleEndian(dataSize));
  }

  private byte[] intToLittleEndian(int value) {
    return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
  }

  private byte[] shortToLittleEndian(short value) {
    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
  }
}
