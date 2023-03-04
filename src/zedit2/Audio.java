package zedit2;

import javax.sound.sampled.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Audio implements Runnable {
    private static final int ZZT_CLOCK_HZ = 1193182;
    private static final double CLOCK_HZ = 1193182.0;
    private static final double PLAYBACK_HZ = 44100.0;
    private static final byte AMPLITUDE = 4;
    private static final int BUFFER_SIZE= 4410;

    private ArrayList<MusicLine> toPlay;
    private SourceDataLine line;
    private int currentPlaying = 0;
    private AudioCallback callback;

    private Thread audioThread;
    private Thread progressThread;
    private boolean stopped = false;

    private ConcurrentLinkedQueue<AudioTiming> timingQueue = new ConcurrentLinkedQueue<>();

    public static Audio playSequence(ArrayList<MusicLine> toPlay, AudioCallback callback) {
        var fmt = new AudioFormat((float) PLAYBACK_HZ, 8, 1, true, false);
        var info = new DataLine.Info(SourceDataLine.class, fmt);
        Audio audio = null;
        try {
            var line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, BUFFER_SIZE);
            audio = new Audio(toPlay, line, callback);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return audio;
    }

    public Audio(ArrayList<MusicLine> toPlay, SourceDataLine line, AudioCallback callback) {
        this.toPlay = toPlay;
        this.line = line;
        this.callback = callback;
        audioThread = new Thread(this);
        audioThread.start();
        progressThread = new Thread() {
            @Override
            public void run() {
                progressRun();
            }
        };
        progressThread.start();
    }

    public static int getPlayDuration(String playMus) {
        short delay = 1;
        int playDur = 0;
        for (int i = 0; i < playMus.length(); i++) {
            char c = Character.toUpperCase(playMus.charAt(i));
            switch (c) {
                case 'T': delay = 1; break;
                case 'S': delay = 2; break;
                case 'I': delay = 4; break;
                case 'Q': delay = 8; break;
                case 'H': delay = 16; break;
                case 'W': delay = 32; break;
                case '3': delay /= 3; break;
                case '.': delay += delay / 2; break;
                case 'C': case 'D': case 'E': case 'F': case 'G': case 'A': case 'B':
                case '0': case '1': case '2': case '4': case '5': case '6': case '7':
                case '8': case '9': case 'X':
                    playDur += delay == 0 ? 256 : delay;
                    break;
                default:
                    break;
            }
        }
        return playDur;
    }

    public void stop() {
        var stopThread = new Thread() {
            @Override
            public void run() {
                stopped = true;
                line.stop();
                line.close();
                try {
                    audioThread.join();
                } catch (InterruptedException ignored) {
                }
                try {
                    progressThread.join();
                } catch (InterruptedException ignored) {
                }
            }
        };
        stopThread.start();
    }

    private void progressRun() {
        while (!line.isRunning()) {
            delay();
        }
        AudioTiming timing = null;
        while (!stopped) {
            delay();
            int frame = line.getFramePosition();
            if (timing == null) {
                timing = timingQueue.poll();
                if (timing == null) continue;
            }
            if (frame >= timing.getBytes()) {
                int seq = timing.getSeq();
                int pos = timing.getPos();
                int len = timing.getLen();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        callback.upTo(seq, pos, len);
                    }
                });

                timing = null;
            };
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                callback.upTo(-1, -1, -1);
            }
        });
    }

    private void delay() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void run() {
        line.start();
        int wavelen = 0;
        while (currentPlaying < toPlay.size()) {
            if (stopped) break;
            wavelen = play(wavelen, toPlay.get(currentPlaying));
            currentPlaying++;
        }
        if (!stopped) {
            stopped = true;
            line.drain();
            line.stop();
            line.close();
        }
    }

    private void addTiming(int pos, int len, int bytes) {
        timingQueue.add(new AudioTiming(currentPlaying, pos, len, bytes));
    }

    private boolean isPlayLine(String code) {
        code = code.replace("?i", "");
        code = code.replace("/i", "");
        code = code.trim().toUpperCase();
        return code.startsWith("#PLAY");
    }

    private int play(int wavelen, MusicLine musicLine) {
        String code = musicLine.seq;

        if (!isPlayLine(code)) return wavelen;
        var waves = new ArrayList<byte[]>();
        int initialWavelen = wavelen;

        var music = MusicNote.fromPlay(code);
        if (music == null) return wavelen;

        for (var mus : music) {
            if (mus.indicate_pos < musicLine.start) continue;
            if (mus.indicate_pos >= musicLine.end) continue;

            double duration = 65535.0 * mus.delay / CLOCK_HZ;
            int totalSamples = (int)(PLAYBACK_HZ * duration);

            int oldWavelen = wavelen;

            if (mus.note >= 0) {
                int tone = (int) Math.pow(2, (mus.octave * 12 + mus.note + 48.0) / 12.0);
                int divi = ZZT_CLOCK_HZ / tone;

                double period = divi * 44100.0 / CLOCK_HZ;

                //System.out.printf("Playing %d (octave %d) for %f seconds\n", divi, octave, duration);
                wavelen = addNote(waves, wavelen, period, totalSamples);
            } else if (mus.drum >= 0) {
                wavelen = addDrum(waves, wavelen, mus.drum, totalSamples);
            } else if (mus.rest) {
                wavelen = addRest(waves, wavelen, totalSamples);
            }
            if (wavelen > oldWavelen) {
                addTiming(mus.indicate_pos, mus.indicate_len, oldWavelen);
            }
        }
        makeNoise(waves, wavelen - initialWavelen);
        return wavelen;
    }

    private int addNote(ArrayList<byte[]> waves, int wavelen, double period, int totalSamples) {
        var wave = new byte[totalSamples];
        int writeFrom = 0;
        double pos = 0.0;

        while (writeFrom < totalSamples) {
            var m = (writeFrom + wavelen) % period;
            byte a;
            int rem;
            if (m < period / 2) {
                a = AMPLITUDE;
                rem = (int)Math.ceil(period / 2 - m);
            } else {
                a = -AMPLITUDE;
                rem = (int)Math.ceil(period - m);
            }
            //System.out.printf("period: %f  m: %f  writeFrom: %d   rem: %d   totalSamples: %d\n", period, m, writeFrom, rem, totalSamples);

            int writeTo = writeFrom + rem;
            writeTo = Math.min(writeTo, totalSamples);
            Arrays.fill(wave, writeFrom, writeTo, a);
            writeFrom = writeTo;
        }
        waves.add(wave);
        wavelen += totalSamples;
        return wavelen;
    }

    private int addRest(ArrayList<byte[]> waves, int wavelen, int totalSamples) {
        var wave = new byte[totalSamples];
        waves.add(wave);
        wavelen += totalSamples;
        return wavelen;
    }

    private int addDrum(ArrayList<byte[]> waves, int wavelen, int drum, int totalSamples) {
        final int[][] drums = {{372},
                {1084, 994, 917, 852, 795, 745, 701, 662, 627, 596, 568, 542, 518, 497},
                {248, 248, 149, 745, 248, 248, 149, 745, 248, 248, 149, 745, 248, 248},
                {},
                {2386, 466, 618, 315, 352, 264, 861, 1081, 243, 351, 1365, 738, 232, 1968},
                {745, 788, 745, 1453, 745, 695, 745, 1309, 745, 606, 745, 800, 745, 692},
                {542, 677, 677, 903, 451, 1355, 542, 677, 677, 903, 451, 1355, 542, 677},
                {1734, 1765, 1796, 1830, 1864, 1899, 1936, 1975, 2015, 2057, 2100, 2146, 2193, 2242},
                {988, 974, 1025, 1058, 1029, 965, 940, 908, 1058, 974, 903, 895, 949, 899},
                {3156, 3604, 3775, 5187, 5326, 3107, 2485, 3728, 3332, 2896, 3173, 1921, 2153, 2800}};
        var drumPattern = drums[drum];

        for (int divi : drumPattern) {
            double period = divi * 44100.0 / CLOCK_HZ;
            int len = 50;
            wavelen = addNote(waves, wavelen, period, len);
            totalSamples -= len;
        }
        wavelen = addRest(waves, wavelen, totalSamples);
        return wavelen;
    }

    private void makeNoise(ArrayList<byte[]> waves, int wavelen) {
        for (var wave : waves) {
            //System.out.printf("Before write: %d\n", line.available());
            //System.out.printf("%s\n", line);
            if (!stopped) {
                line.write(wave, 0, wave.length);
            }
            //System.out.printf("After write: %d\n", line.available());

        }
    }
}
