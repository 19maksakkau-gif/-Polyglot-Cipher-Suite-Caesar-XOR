// caesar_xor.go
package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"
	"unicode"
)

// ANSI colors
const (
	reset  = "\033[0m"
	green  = "\033[92m"
	red    = "\033[91m"
	yellow = "\033[93m"
	blue   = "\033[94m"
)

func colorize(text, color string) string {
	return color + text + reset
}

// Caesar encrypt (supports ASCII letters only for simplicity, but can be extended)
func caesarEncrypt(text string, shift int) string {
	shift = shift % 26
	res := make([]rune, 0, len(text))
	for _, ch := range text {
		if ch >= 'A' && ch <= 'Z' {
			res = append(res, rune('A'+(int(ch-'A')+shift)%26))
		} else if ch >= 'a' && ch <= 'z' {
			res = append(res, rune('a'+(int(ch-'a')+shift)%26))
		} else {
			res = append(res, ch)
		}
	}
	return string(res)
}

func caesarDecrypt(text string, shift int) string {
	return caesarEncrypt(text, -shift)
}

func caesarBruteforce(text string) []struct {
	shift int
	dec   string
	score int
} {
	candidates := make([]struct {
		shift int
		dec   string
		score int
	}, 26)
	for s := 0; s < 26; s++ {
		dec := caesarEncrypt(text, -s)
		score := 0
		for _, ch := range strings.ToLower(dec) {
			if strings.ContainsRune("etaoinshrdlu", ch) {
				score++
			}
		}
		candidates[s] = struct {
			shift int
			dec   string
			score int
		}{s, dec, score}
	}
	// sort by score descending (simple bubble)
	for i := 0; i < 25; i++ {
		for j := i + 1; j < 26; j++ {
			if candidates[j].score > candidates[i].score {
				candidates[i], candidates[j] = candidates[j], candidates[i]
			}
		}
	}
	return candidates
}

// XOR encrypt/decrypt (symmetrical)
func xorCipher(text, key string) string {
	if key == "" {
		panic("key cannot be empty")
	}
	keyBytes := []byte(key)
	textBytes := []byte(text)
	res := make([]byte, len(textBytes))
	for i, b := range textBytes {
		res[i] = b ^ keyBytes[i%len(keyBytes)]
	}
	return string(res)
}

func readFile(filename string) (string, error) {
	data, err := os.ReadFile(filename)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func writeFile(filename, content string) error {
	return os.WriteFile(filename, []byte(content), 0644)
}

func main() {
	encrypt := flag.Bool("e", false, "Encrypt mode (default)")
	decrypt := flag.Bool("d", false, "Decrypt mode")
	caesar := flag.Bool("c", false, "Use Caesar cipher")
	xor := flag.Bool("x", false, "Use XOR cipher")
	key := flag.String("k", "", "Key: shift (int) for Caesar or string for XOR")
	text := flag.String("t", "", "Input text")
	input := flag.String("i", "", "Input file")
	output := flag.String("o", "", "Output file")
	bruteforce := flag.Bool("b", false, "Bruteforce Caesar (ignores key)")
	flag.Parse()

	// Mode validation
	if *decrypt && *encrypt {
		fmt.Println(colorize("Error: cannot use both -e and -d", red))
		os.Exit(1)
	}
	mode := "encrypt"
	if *decrypt {
		mode = "decrypt"
	}
	if *caesar && *xor {
		fmt.Println(colorize("Error: choose only one cipher (-c or -x)", red))
		os.Exit(1)
	}
	if !*caesar && !*xor {
		fmt.Println(colorize("Error: specify cipher (-c or -x)", red))
		os.Exit(1)
	}

	// Read input
	var inputText string
	if *text != "" {
		inputText = *text
	} else if *input != "" {
		content, err := readFile(*input)
		if err != nil {
			fmt.Println(colorize("Error reading file: "+err.Error(), red))
			os.Exit(1)
		}
		inputText = content
	} else {
		// read from stdin
		info, _ := os.Stdin.Stat()
		if (info.Mode() & os.ModeCharDevice) == 0 {
			// data from pipe
			reader := bufio.NewReader(os.Stdin)
			var builder strings.Builder
			for {
				line, err := reader.ReadString('\n')
				if err != nil && err != io.EOF {
					fmt.Println(colorize("Error reading stdin: "+err.Error(), red))
					os.Exit(1)
				}
				builder.WriteString(line)
				if err == io.EOF {
					break
				}
			}
			inputText = builder.String()
		} else {
			fmt.Print(colorize("Enter text (end with Ctrl+D): ", yellow))
			reader := bufio.NewReader(os.Stdin)
			var builder strings.Builder
			for {
				line, err := reader.ReadString('\n')
				if err != nil && err != io.EOF {
					fmt.Println(colorize("Error reading stdin: "+err.Error(), red))
					os.Exit(1)
				}
				builder.WriteString(line)
				if err == io.EOF {
					break
				}
			}
			inputText = builder.String()
		}
	}
	if len(inputText) == 0 {
		fmt.Println(colorize("Error: no input text", red))
		os.Exit(1)
	}

	// Process
	var result string
	defer func() {
		if r := recover(); r != nil {
			fmt.Println(colorize(fmt.Sprintf("Panic: %v", r), red))
			os.Exit(1)
		}
	}()
	if *caesar {
		if *bruteforce {
			fmt.Println(colorize("Bruteforce Caesar mode:", blue))
			candidates := caesarBruteforce(inputText)
			for i, cand := range candidates {
				if i >= 5 {
					break
				}
				fmt.Printf("Shift %2d (score %d): %s\n", cand.shift, cand.score, colorize(cand.dec, green))
			}
			best := candidates[0]
			fmt.Println(colorize(fmt.Sprintf("\nMost likely shift: %d", best.shift), yellow))
			fmt.Println(colorize("Result: "+best.dec, green))
			result = best.dec
		} else {
			shift, err := strconv.Atoi(*key)
			if err != nil {
				fmt.Println(colorize("Key for Caesar must be an integer", red))
				os.Exit(1)
			}
			if mode == "encrypt" {
				result = caesarEncrypt(inputText, shift)
			} else {
				result = caesarDecrypt(inputText, shift)
			}
		}
	} else { // XOR
		if *key == "" {
			fmt.Println(colorize("Key for XOR cannot be empty", red))
			os.Exit(1)
		}
		result = xorCipher(inputText, *key) // XOR is symmetric
	}

	// Output
	if *output != "" {
		err := writeFile(*output, result)
		if err != nil {
			fmt.Println(colorize("Error writing file: "+err.Error(), red))
			os.Exit(1)
		}
		fmt.Println(colorize("Result written to "+*output, green))
	} else {
		fmt.Print(result)
	}
}
