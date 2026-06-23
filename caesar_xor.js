// caesar_xor.js
#!/usr/bin/env node
'use strict';

const fs = require('fs');
const readline = require('readline');

// ANSI colors
const COLORS = {
    green: '\x1b[92m',
    red: '\x1b[91m',
    yellow: '\x1b[93m',
    blue: '\x1b[94m',
    reset: '\x1b[0m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

// Caesar functions
function caesarEncrypt(text, shift) {
    shift = ((shift % 26) + 26) % 26;
    return text.replace(/[A-Za-z]/g, (ch) => {
        const base = ch >= 'a' ? 'a'.charCodeAt(0) : 'A'.charCodeAt(0);
        const code = ch.charCodeAt(0);
        return String.fromCharCode(((code - base + shift) % 26) + base);
    });
}

function caesarDecrypt(text, shift) {
    return caesarEncrypt(text, -shift);
}

function caesarBruteforce(text) {
    const candidates = [];
    for (let s = 0; s < 26; s++) {
        const dec = caesarEncrypt(text, -s);
        let score = 0;
        for (const ch of dec.toLowerCase()) {
            if ('etaoinshrdlu'.includes(ch)) score++;
        }
        candidates.push({ shift: s, dec, score });
    }
    candidates.sort((a, b) => b.score - a.score);
    return candidates;
}

// XOR (symmetrical)
function xorCipher(text, key) {
    if (!key) throw new Error('Key cannot be empty');
    const keyBytes = Buffer.from(key, 'utf8');
    const textBytes = Buffer.from(text, 'utf8');
    const result = Buffer.alloc(textBytes.length);
    for (let i = 0; i < textBytes.length; i++) {
        result[i] = textBytes[i] ^ keyBytes[i % keyBytes.length];
    }
    return result.toString('utf8');
}

function readFile(filename) {
    return fs.readFileSync(filename, 'utf8');
}

function writeFile(filename, content) {
    fs.writeFileSync(filename, content, 'utf8');
}

function parseArgs() {
    const args = process.argv.slice(2);
    const opts = {
        encrypt: false,
        decrypt: false,
        caesar: false,
        xor: false,
        key: '',
        text: '',
        input: '',
        output: '',
        bruteforce: false,
    };
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        switch (arg) {
            case '-e':
            case '--encrypt': opts.encrypt = true; break;
            case '-d':
            case '--decrypt': opts.decrypt = true; break;
            case '-c':
            case '--caesar': opts.caesar = true; break;
            case '-x':
            case '--xor': opts.xor = true; break;
            case '-k':
            case '--key': opts.key = args[++i]; break;
            case '-t':
            case '--text': opts.text = args[++i]; break;
            case '-i':
            case '--input': opts.input = args[++i]; break;
            case '-o':
            case '--output': opts.output = args[++i]; break;
            case '-b':
            case '--bruteforce': opts.bruteforce = true; break;
            case '-h':
            case '--help':
                console.log(`Usage: node caesar_xor.js [options]
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
  -h, --help          Show this help`);
                process.exit(0);
            default:
                console.error(colorize(`Unknown option: ${arg}`, 'red'));
                process.exit(1);
        }
    }
    return opts;
}

async function main() {
    const opts = parseArgs();

    if (opts.decrypt && opts.encrypt) {
        console.error(colorize('Error: cannot use both -e and -d', 'red'));
        process.exit(1);
    }
    const mode = opts.decrypt ? 'decrypt' : 'encrypt';

    if (opts.caesar && opts.xor) {
        console.error(colorize('Error: choose only one cipher (-c or -x)', 'red'));
        process.exit(1);
    }
    if (!opts.caesar && !opts.xor) {
        console.error(colorize('Error: specify cipher (-c or -x)', 'red'));
        process.exit(1);
    }

    // Read input
    let inputText = '';
    if (opts.text) {
        inputText = opts.text;
    } else if (opts.input) {
        try {
            inputText = readFile(opts.input);
        } catch (err) {
            console.error(colorize(`Error reading file: ${err.message}`, 'red'));
            process.exit(1);
        }
    } else {
        // read from stdin
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout,
            terminal: false
        });
        const lines = [];
        for await (const line of rl) {
            lines.push(line);
        }
        inputText = lines.join('\n');
        if (!inputText && process.stdin.isTTY) {
            console.error(colorize('Error: no input text', 'red'));
            process.exit(1);
        }
    }

    let result = '';
    try {
        if (opts.caesar) {
            if (opts.bruteforce) {
                console.log(colorize('Bruteforce Caesar mode:', 'blue'));
                const candidates = caesarBruteforce(inputText);
                for (let i = 0; i < Math.min(5, candidates.length); i++) {
                    const c = candidates[i];
                    console.log(`Shift ${c.shift.toString().padStart(2)} (score ${c.score}): ${colorize(c.dec, 'green')}`);
                }
                const best = candidates[0];
                console.log(colorize(`\nMost likely shift: ${best.shift}`, 'yellow'));
                console.log(colorize(`Result: ${best.dec}`, 'green'));
                result = best.dec;
            } else {
                const shift = parseInt(opts.key);
                if (isNaN(shift)) {
                    console.error(colorize('Key for Caesar must be an integer', 'red'));
                    process.exit(1);
                }
                if (mode === 'encrypt') {
                    result = caesarEncrypt(inputText, shift);
                } else {
                    result = caesarDecrypt(inputText, shift);
                }
            }
        } else { // XOR
            if (!opts.key) {
                console.error(colorize('Key for XOR cannot be empty', 'red'));
                process.exit(1);
            }
            result = xorCipher(inputText, opts.key);
        }
    } catch (err) {
        console.error(colorize(`Error: ${err.message}`, 'red'));
        process.exit(1);
    }

    if (opts.output) {
        try {
            writeFile(opts.output, result);
            console.log(colorize(`Result written to ${opts.output}`, 'green'));
        } catch (err) {
            console.error(colorize(`Error writing file: ${err.message}`, 'red'));
            process.exit(1);
        }
    } else {
        process.stdout.write(result);
    }
}

main().catch(err => {
    console.error(colorize(`Unhandled error: ${err.message}`, 'red'));
    process.exit(1);
});
