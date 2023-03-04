package zedit2;

import java.util.ArrayList;

public class MusicNote {
    public int delay = 0;
    public int note = -1;
    public int drum = -1;
    public int octave = 0;
    public int indicate_pos = 0;
    public int indicate_len = 0;
    public boolean rest = false;
    public String original;
    public int desired_octave = 0;

    public static int fixOctavesFor(int cursor, ArrayList<MusicNote> musicNotes) {
        // See if we don't need to fix octaves
        if (!musicNotes.get(cursor).isTransposable()) return cursor;
        if (musicNotes.get(cursor).desired_octave == musicNotes.get(cursor).octave) return cursor;

        // Loop backwards, removing all octave changes between this note and the last playable note
        boolean erasing;
        int prevPlayableOctave = 4;
        do {
            erasing = false;
            for (int i = cursor - 1; i >= 0; i--) {
                if (musicNotes.get(i).isTransposable()) {
                    prevPlayableOctave = musicNotes.get(i).octave;
                    break;
                }
                if (musicNotes.get(i).original.equals("+") || musicNotes.get(i).original.equals("-")) {
                    musicNotes.remove(i);
                    cursor--;
                    erasing = true;
                    break;
                }
            }
        } while (erasing);

        // Now add more +s and -s to balance everything out
        while (prevPlayableOctave != musicNotes.get(cursor).desired_octave) {
            int changeBy;
            if (prevPlayableOctave < musicNotes.get(cursor).desired_octave) {
                changeBy = 1;
            } else {
                changeBy = -1;
            }
            prevPlayableOctave += changeBy;
            var octaveChange = new MusicNote();
            octaveChange.indicate_pos = musicNotes.get(cursor).indicate_pos;
            octaveChange.indicate_len = 1;
            octaveChange.octave = prevPlayableOctave;
            octaveChange.desired_octave = octaveChange.octave;
            octaveChange.original = changeBy == 1 ? "+" : "-";
            musicNotes.get(cursor).indicate_pos++;
            musicNotes.add(cursor, octaveChange);
            cursor++;
        }

        // Now loop across the entire #play sequence and fix the octave #s up
        int octave = 4;
        for (int i = 0; i < musicNotes.size(); i++) {
            switch (musicNotes.get(i).original) {
                case "+":
                    octave = Math.min(octave + 1, 7);
                    break;
                case "-":
                    octave = Math.max(octave - 1, 2);
                    break;
                default:
                    break;
            }
            musicNotes.get(i).octave = octave;
        }

        return cursor;
    }

    public boolean isTransposable() {
        return note >= 0;
    }

    public static ArrayList<MusicNote> fromPlay(String code) {
        int start = code.toUpperCase().indexOf("#PLAY");
        if (start == -1) return null;
        start += 5;

        ArrayList<MusicNote> music = new ArrayList<>();
        short delay = 1;
        int octave = 4;

        for (int pos = start; pos < code.length(); pos++) {
            int indicate_pos = pos;
            int indicate_len = 1;
            int note = -1;
            int drum = -1;
            boolean rest = false;

            switch (Character.toUpperCase(code.charAt(pos))) {
                case 'T':
                    delay = 1;
                    break;
                case 'S':
                    delay = 2;
                    break;
                case 'I':
                    delay = 4;
                    break;
                case 'Q':
                    delay = 8;
                    break;
                case 'H':
                    delay = 16;
                    break;
                case 'W':
                    delay = 32;
                    break;
                case '3':
                    delay /= 3;
                    break;
                case '.':
                    delay += delay / 2;
                    break;
                case '+':
                    octave = Math.min(octave + 1, 7);
                    break;
                case '-':
                    octave = Math.max(octave - 1, 2);
                    break;
                case 'X':
                    rest = true;
                    break;
                case 'C':
                    note = 0;
                    break;
                case 'D':
                    note = 2;
                    break;
                case 'E':
                    note = 4;
                    break;
                case 'F':
                    note = 5;
                    break;
                case 'G':
                    note = 7;
                    break;
                case 'A':
                    note = 9;
                    break;
                case 'B':
                    note = 11;
                    break;
                case '#':
                    break;
                case '!':
                    break;
                case '0':
                    drum = 0;
                    break;
                case '1':
                    drum = 1;
                    break;
                case '2':
                    drum = 2;
                    break;
                case '4':
                    drum = 4;
                    break;
                case '5':
                    drum = 5;
                    break;
                case '6':
                    drum = 6;
                    break;
                case '7':
                    drum = 7;
                    break;
                case '8':
                    drum = 8;
                    break;
                case '9':
                    drum = 9;
                    break;
                default:
                    break;
            }
            if (pos + 1 < code.length() && note >= 0) {
                char suffix = code.charAt(pos + 1);
                if (suffix == '#') {
                    note++;
                    indicate_len = 2;
                } else if (suffix == '!') {
                    note--;
                    indicate_len = 2;
                }
            }

            if ((note < 0) || (note > 11)) note = -1;

            if (delay == 0) delay = 256; // In ZZT, a delay of 0 plays for this long

            MusicNote mus = new MusicNote();
            mus.delay = delay;
            mus.rest = rest;
            mus.note = note;
            mus.drum = drum;
            mus.octave = octave;
            mus.desired_octave = octave;
            mus.indicate_pos = indicate_pos;
            mus.indicate_len = indicate_len;
            mus.original = code.substring(pos, pos + indicate_len);
            music.add(mus);

            pos += indicate_len - 1;
        }
        return music;
    }

    public boolean transpose(int by) {
        if (by != 1 && by != -1) throw new IllegalArgumentException("Can only be transposed by 1 semitone");
        if (note == -1) return true;

        int new_note = note + by;
        int new_octave = octave;
        if (new_note < 0) {
            new_octave--;
            new_note += 12;
        } else if (new_note > 11) {
            new_octave++;
            new_note -= 12;
        }
        if (new_octave < 2 || new_octave > 7) return false;
        note = new_note;
        // Was the original note string in uppercase? If so, keep it in uppercase
        boolean upper = false;
        if (Character.isUpperCase(original.charAt(0))) upper = true;
        String noteString = "?";

        switch (note) {
            case 0: noteString = "c"; break;
            case 1: noteString = "c#"; break;
            case 2: noteString = "d"; break;
            case 3: noteString = "d#"; break;
            case 4: noteString = "e"; break;
            case 5: noteString = "f"; break;
            case 6: noteString = "f#"; break;
            case 7: noteString = "g"; break;
            case 8: noteString = "g#"; break;
            case 9: noteString = "a"; break;
            case 10: noteString = "a#"; break;
            case 11: noteString = "b"; break;
        }
        if (upper) noteString = noteString.toUpperCase();
        original = noteString;

        desired_octave = new_octave;
        return true;
    }
}
