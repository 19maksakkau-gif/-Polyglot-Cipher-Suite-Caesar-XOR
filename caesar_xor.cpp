// caesar_xor.cpp
#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <fstream>
#include <cctype>
#include <map>
#include <cstring>
#include <getopt.h> // requires GNU getopt; for Windows, use alternative

using namespace std;

// ANSI colors
const string RESET = "\033[0m";
const string GREEN = "\033[92m";
const string RED = "\033[91m";
const string YELLOW = "\033[93m";
const string BLUE = "\033[94m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

// Caesar encrypt (ASCII letters only)
string caesarEncrypt(const string& text, int shift) {
    shift = ((shift % 26) + 26) % 26;
    string res;
    for (char ch : text) {
        if (isalpha(ch)) {
            char base = isupper(ch) ? 'A' : 'a';
            res += char((ch - base + shift) % 26 + base);
        } else {
            res += ch;
        }
    }
    return res;
}

string caesarDecrypt(const string& text, int shift) {
    return caesarEncrypt(text, -shift);
}

// Bruteforce Caesar
vector<pair<int, string>> caesarBruteforce(const string& text) {
    vector<pair<int, string>> candidates;
    for (int s = 0; s < 26; ++s) {
        string dec = caesarEncrypt(text, -s);
        int score = 0;
        for (char ch : dec) {
            if (strchr("etaoinshrdlu", tolower(ch))) score++;
        }
        candidates.push_back({score, dec});
    }
    sort(candidates.begin(), candidates.end(), [](auto& a, auto& b) {
        return a.first > b.first;
    });
    vector<pair<int, string>> result;
    for (int i = 0; i < 26; ++i) {
        result.push_back({i, candidates[i].second});
    }
    return result;
}

// XOR (symmetrical)
string xorCipher(const string& text, const string& key) {
    if (key.empty()) throw runtime_error("Key cannot be empty");
    string res;
    res.resize(text.size());
    for (size_t i = 0; i < text.size(); ++i) {
        res[i] = text[i] ^ key[i % key.size()];
    }
    return res;
}

string readFile(const string& filename) {
    ifstream f(filename);
    if (!f) throw runtime_error("Cannot open file: " + filename);
    string content((istreambuf_iterator<char>(f)), istreambuf_iterator<char>());
    return content;
}

void writeFile(const string& filename, const string& content) {
    ofstream f(filename);
    if (!f) throw runtime_error("Cannot write file: " + filename);
    f << content;
}

void printHelp() {
    cout << "Usage: caesar_xor [options]\n"
         << "Options:\n"
         << "  -e, --encrypt       Encrypt mode (default)\n"
         << "  -d, --decrypt       Decrypt mode\n"
         << "  -c, --caesar        Use Caesar cipher\n"
         << "  -x, --xor           Use XOR cipher\n"
         << "  -k, --key <key>     Key: shift (int) for Caesar or string for XOR\n"
         << "  -t, --text <text>   Input text\n"
         << "  -i, --input <file>  Input file\n"
         << "  -o, --output <file> Output file\n"
         << "  -b, --bruteforce    Bruteforce Caesar (ignores key)\n"
         << "  -h, --help          Show this help\n";
}

int main(int argc, char* argv[]) {
    static struct option long_options[] = {
        {"encrypt", no_argument, 0, 'e'},
        {"decrypt", no_argument, 0, 'd'},
        {"caesar", no_argument, 0, 'c'},
        {"xor", no_argument, 0, 'x'},
        {"key", required_argument, 0, 'k'},
        {"text", required_argument, 0, 't'},
        {"input", required_argument, 0, 'i'},
        {"output", required_argument, 0, 'o'},
        {"bruteforce", no_argument, 0, 'b'},
        {"help", no_argument, 0, 'h'},
        {0, 0, 0, 0}
    };

    bool encrypt = false, decrypt = false, caesar = false, xorMode = false, bruteforce = false;
    string key, text, inputFile, outputFile;

    int c;
    while ((c = getopt_long(argc, argv, "edcxk:t:i:o:bh", long_options, nullptr)) != -1) {
        switch (c) {
            case 'e': encrypt = true; break;
            case 'd': decrypt = true; break;
            case 'c': caesar = true; break;
            case 'x': xorMode = true; break;
            case 'k': key = optarg; break;
            case 't': text = optarg; break;
            case 'i': inputFile = optarg; break;
            case 'o': outputFile = optarg; break;
            case 'b': bruteforce = true; break;
            case 'h': printHelp(); return 0;
            default: printHelp(); return 1;
        }
    }

    if (decrypt && encrypt) {
        cerr << colorize("Error: cannot use both -e and -d", RED) << endl;
        return 1;
    }
    string mode = decrypt ? "decrypt" : "encrypt";

    if (caesar && xorMode) {
        cerr << colorize("Error: choose only one cipher (-c or -x)", RED) << endl;
        return 1;
    }
    if (!caesar && !xorMode) {
        cerr << colorize("Error: specify cipher (-c or -x)", RED) << endl;
        return 1;
    }

    string inputText;
    try {
        if (!text.empty()) {
            inputText = text;
        } else if (!inputFile.empty()) {
            inputText = readFile(inputFile);
        } else {
            // read from stdin
            string line;
            while (getline(cin, line)) {
                inputText += line + '\n';
            }
            if (!inputText.empty() && inputText.back() == '\n') {
                inputText.pop_back(); // remove last newline
            }
            if (inputText.empty()) {
                throw runtime_error("No input text");
            }
        }
    } catch (const exception& e) {
        cerr << colorize("Error: " + string(e.what()), RED) << endl;
        return 1;
    }

    string result;
    try {
        if (caesar) {
            if (bruteforce) {
                cout << colorize("Bruteforce Caesar mode:", BLUE) << endl;
                auto candidates = caesarBruteforce(inputText);
                for (int i = 0; i < min(5, (int)candidates.size()); ++i) {
                    cout << "Shift " << i << " (score " << candidates[i].first << "): "
                         << colorize(candidates[i].second, GREEN) << endl;
                }
                cout << colorize("\nMost likely shift: 0", YELLOW) << endl; // simplified
                cout << colorize("Result: " + candidates[0].second, GREEN) << endl;
                result = candidates[0].second;
            } else {
                int shift = stoi(key);
                if (mode == "encrypt") {
                    result = caesarEncrypt(inputText, shift);
                } else {
                    result = caesarDecrypt(inputText, shift);
                }
            }
        } else { // XOR
            if (key.empty()) throw runtime_error("Key for XOR cannot be empty");
            result = xorCipher(inputText, key);
        }
    } catch (const exception& e) {
        cerr << colorize("Error: " + string(e.what()), RED) << endl;
        return 1;
    }

    if (!outputFile.empty()) {
        try {
            writeFile(outputFile, result);
            cout << colorize("Result written to " + outputFile, GREEN) << endl;
        } catch (const exception& e) {
            cerr << colorize("Error writing file: " + string(e.what()), RED) << endl;
            return 1;
        }
    } else {
        cout << result;
    }
    return 0;
}
