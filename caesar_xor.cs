// caesar_xor.cs
using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Collections.Generic;

class CaesarXorTool
{
    // ANSI colors (if supported)
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "green" => "\x1b[92m",
            "red" => "\x1b[91m",
            "yellow" => "\x1b[93m",
            "blue" => "\x1b[94m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    // Caesar
    static string CaesarEncrypt(string text, int shift)
    {
        shift = ((shift % 26) + 26) % 26;
        var res = new StringBuilder();
        foreach (char ch in text)
        {
            if (char.IsLetter(ch))
            {
                char baseCh = char.IsUpper(ch) ? 'A' : 'a';
                res.Append((char)((ch - baseCh + shift) % 26 + baseCh));
            }
            else res.Append(ch);
        }
        return res.ToString();
    }

    static string CaesarDecrypt(string text, int shift) => CaesarEncrypt(text, -shift);

    static List<(int shift, string dec, int score)> CaesarBruteforce(string text)
    {
        var candidates = new List<(int, string, int)>();
        for (int s = 0; s < 26; s++)
        {
            string dec = CaesarEncrypt(text, -s);
            int score = dec.Count(ch => "etaoinshrdlu".Contains(char.ToLower(ch)));
            candidates.Add((s, dec, score));
        }
        candidates.Sort((a, b) => b.score.CompareTo(a.score));
        return candidates;
    }

    // XOR
    static string XorCipher(string text, string key)
    {
        if (string.IsNullOrEmpty(key)) throw new ArgumentException("Key cannot be empty");
        byte[] keyBytes = Encoding.UTF8.GetBytes(key);
        byte[] textBytes = Encoding.UTF8.GetBytes(text);
        byte[] result = new byte[textBytes.Length];
        for (int i = 0; i < textBytes.Length; i++)
            result[i] = (byte)(textBytes[i] ^ keyBytes[i % keyBytes.Length]);
        return Encoding.UTF8.GetString(result);
    }

    static string ReadFile(string filename) => File.ReadAllText(filename, Encoding.UTF8);
    static void WriteFile(string filename, string content) => File.WriteAllText(filename, content, Encoding.UTF8);

    static void Main(string[] args)
    {
        bool encrypt = false, decrypt = false, caesar = false, xorMode = false, bruteforce = false;
        string key = "", text = "", inputFile = "", outputFile = "";

        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            switch (arg)
            {
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
                case "--help":
                    Console.WriteLine(@"Usage: caesar_xor [options]
Options:
  -e, --encrypt       Encrypt mode (default)
  -d, --decrypt       Decrypt mode
  -c, --caesar        Use Caesar cipher
  -x, --xor           Use XOR cipher
  -k, --key <key>     Key: shift (int) for Caesar or string for XOR
  -t, --text <text>   Input text
  -i, --input <file>  Input file
  -o, --output <file> Output file
  -b, --bruteforce    Bruteforce Caesar (ignores key)
  -h, --help          Show this help");
                    return;
                default:
                    Console.WriteLine(Colorize($"Unknown option: {arg}", "red"));
                    Environment.Exit(1);
                    break;
            }
        }

        if (decrypt && encrypt) { Console.WriteLine(Colorize("Error: cannot use both -e and -d", "red")); Environment.Exit(1); }
        string mode = decrypt ? "decrypt" : "encrypt";
        if (caesar && xorMode) { Console.WriteLine(Colorize("Error: choose only one cipher (-c or -x)", "red")); Environment.Exit(1); }
        if (!caesar && !xorMode) { Console.WriteLine(Colorize("Error: specify cipher (-c or -x)", "red")); Environment.Exit(1); }

        string inputText;
        try
        {
            if (!string.IsNullOrEmpty(text)) inputText = text;
            else if (!string.IsNullOrEmpty(inputFile)) inputText = ReadFile(inputFile);
            else
            {
                // read from stdin
                using var reader = new StreamReader(Console.OpenStandardInput(), Encoding.UTF8);
                inputText = reader.ReadToEnd();
                if (string.IsNullOrEmpty(inputText))
                {
                    Console.WriteLine(Colorize("No input text", "red"));
                    Environment.Exit(1);
                }
            }
        }
        catch (Exception e)
        {
            Console.WriteLine(Colorize($"Error reading input: {e.Message}", "red"));
            Environment.Exit(1);
            return;
        }

        string result = "";
        try
        {
            if (caesar)
            {
                if (bruteforce)
                {
                    Console.WriteLine(Colorize("Bruteforce Caesar mode:", "blue"));
                    var candidates = CaesarBruteforce(inputText);
                    for (int i = 0; i < Math.Min(5, candidates.Count); i++)
                    {
                        Console.WriteLine($"Shift {candidates[i].shift,2} (score {candidates[i].score}): {Colorize(candidates[i].dec, "green")}");
                    }
                    var best = candidates[0];
                    Console.WriteLine(Colorize($"\nMost likely shift: {best.shift}", "yellow"));
                    Console.WriteLine(Colorize($"Result: {best.dec}", "green"));
                    result = best.dec;
                }
                else
                {
                    if (!int.TryParse(key, out int shift))
                    {
                        Console.WriteLine(Colorize("Key for Caesar must be an integer", "red"));
                        Environment.Exit(1);
                    }
                    result = mode == "encrypt" ? CaesarEncrypt(inputText, shift) : CaesarDecrypt(inputText, shift);
                }
            }
            else // XOR
            {
                if (string.IsNullOrEmpty(key)) throw new ArgumentException("Key for XOR cannot be empty");
                result = XorCipher(inputText, key);
            }
        }
        catch (Exception e)
        {
            Console.WriteLine(Colorize($"Error: {e.Message}", "red"));
            Environment.Exit(1);
        }

        if (!string.IsNullOrEmpty(outputFile))
        {
            try { WriteFile(outputFile, result); Console.WriteLine(Colorize($"Result written to {outputFile}", "green")); }
            catch (Exception e) { Console.WriteLine(Colorize($"Error writing file: {e.Message}", "red")); Environment.Exit(1); }
        }
        else
        {
            Console.Write(result);
        }
    }
}
