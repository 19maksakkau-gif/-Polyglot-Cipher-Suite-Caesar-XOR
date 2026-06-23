// caesar_xor.java
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class caesar_xor {
    // ANSI colors (if terminal supports)
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[93m";
    private static final String BLUE = "\u001B[94m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    // Caesar
    private static String caesarEncrypt(String text, int shift) {
        shift = ((shift % 26) + 26) % 26;
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                sb.append((char)((ch - base + shift) % 26 + base));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String caesarDecrypt(String text, int shift) {
        return caesarEncrypt(text, -shift);
    }

    private static List<Map.Entry<Integer, String>> caesarBruteforce(String text) {
        Map<Integer, Integer> scores = new HashMap<>();
        List<Map.Entry<Integer, String>> candidates = new ArrayList<>();
        for (int s = 0; s < 26; s++) {
            String dec = caesarEncrypt(text, -s);
            int score = 0;
            for (char ch : dec.toLowerCase().toCharArray()) {
                if ("etaoinshrdlu".indexOf(ch) != -1) score++;
            }
            candidates.add(Map.entry(s, dec));
            scores.put(s, score);
        }
        candidates.sort((a, b) -> scores.get(b.getKey()) - scores.get(a.getKey()));
        return candidates;
    }

    // XOR (symmetrical)
    private static String xorCipher(String text, String key) {
        if (key.isEmpty()) throw new IllegalArgumentException("Key cannot be empty");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[textBytes.length];
        for (int i = 0; i < textBytes.length; i++) {
            result[i] = (byte)(textBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    private static String readFile(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
    }

    private static void writeFile(String filename, String content) throws IOException {
        Files.write(Paths.get(filename), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void printHelp() {
        System.out.println("Usage: java caesar_xor [options]");
        System.out.println("Options:");
        System.out.println("  -e, --encrypt       Encrypt mode (default)");
        System.out.println("  -d, --decrypt       Decrypt mode");
        System.out.println("  -c, --caesar        Use Caesar cipher");
        System.out.println("  -x, --xor           Use XOR cipher");
        System.out.println("  -k, --key <key>     Key: shift (int) for Caesar or string for XOR");
        System.out.println("  -t, --text <text>   Input text");
        System.out.println("  -i, --input <file>  Input file");
        System.out.println("  -o, --output <file> Output file");
        System.out.println("  -b, --bruteforce    Bruteforce Caesar (ignores key)");
        System.out.println("  -h, --help          Show this help");
    }

    public static void main(String[] args) {
        boolean encrypt = false, decrypt = false, caesar = false, xorMode = false, bruteforce = false;
        String key = "", text = "", inputFile = "", outputFile = "";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e":
                case "--encrypt": encrypt = true; break;
                case "-d":
                case "--decrypt": decrypt = true; break;
                case "-c":
                case "--caesar": caesar = true; break;
                case "-x":
                case "--xor": xorMode = true; break;
                case "-k":
                case "--key": key = args[++i]; break;
                case "-t":
                case "--text": text = args[++i]; break;
                case "-i":
                case "--input": inputFile = args[++i]; break;
                case "-o":
                case "--output": outputFile = args[++i]; break;
                case "-b":
                case "--bruteforce": bruteforce = true; break;
                case "-h":
                case "--help": printHelp(); return;
                default:
                    System.err.println(colorize("Unknown option: " + args[i], RED));
                    System.exit(1);
            }
        }

        if (decrypt && encrypt) { System.err.println(colorize("Error: cannot use both -e and -d", RED)); System.exit(1); }
        String mode = decrypt ? "decrypt" : "encrypt";
        if (caesar && xorMode) { System.err.println(colorize("Error: choose only one cipher (-c or -x)", RED)); System.exit(1); }
        if (!caesar && !xorMode) { System.err.println(colorize("Error: specify cipher (-c or -x)", RED)); System.exit(1); }

        String inputText;
        try {
            if (!text.isEmpty()) {
                inputText = text;
            } else if (!inputFile.isEmpty()) {
                inputText = readFile(inputFile);
            } else {
                // read from stdin
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                inputText = sb.toString();
                if (!inputText.isEmpty() && inputText.charAt(inputText.length()-1) == '\n') {
                    inputText = inputText.substring(0, inputText.length()-1);
                }
                if (inputText.isEmpty()) {
                    System.err.println(colorize("No input text", RED));
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            System.err.println(colorize("Error reading input: " + e.getMessage(), RED));
            System.exit(1);
            return;
        }

        String result = "";
        try {
            if (caesar) {
                if (bruteforce) {
                    System.out.println(colorize("Bruteforce Caesar mode:", BLUE));
                    List<Map.Entry<Integer, String>> candidates = caesarBruteforce(inputText);
                    for (int i = 0; i < Math.min(5, candidates.size()); i++) {
                        System.out.printf("Shift %2d (score %d): %s%n",
                                candidates.get(i).getKey(),
                                // score not stored; recalc
                                (int)candidates.get(i).getValue().chars().filter(ch -> "etaoinshrdlu".indexOf(Character.toLowerCase(ch)) != -1).count(),
                                colorize(candidates.get(i).getValue(), GREEN));
                    }
                    Map.Entry<Integer, String> best = candidates.get(0);
                    System.out.println(colorize("\nMost likely shift: " + best.getKey(), YELLOW));
                    System.out.println(colorize("Result: " + best.getValue(), GREEN));
                    result = best.getValue();
                } else {
                    int shift = Integer.parseInt(key);
                    result = mode.equals("encrypt") ? caesarEncrypt(inputText, shift) : caesarDecrypt(inputText, shift);
                }
            } else { // XOR
                if (key.isEmpty()) throw new IllegalArgumentException("Key for XOR cannot be empty");
                result = xorCipher(inputText, key);
            }
        } catch (Exception e) {
            System.err.println(colorize("Error: " + e.getMessage(), RED));
            System.exit(1);
        }

        if (!outputFile.isEmpty()) {
            try {
                writeFile(outputFile, result);
                System.out.println(colorize("Result written to " + outputFile, GREEN));
            } catch (IOException e) {
                System.err.println(colorize("Error writing file: " + e.getMessage(), RED));
                System.exit(1);
            }
        } else {
            System.out.print(result);
        }
    }
}
