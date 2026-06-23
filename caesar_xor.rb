#!/usr/bin/env ruby
# caesar_xor.rb
# encoding: UTF-8

require 'optparse'
require 'io/console'

# ANSI colors
COLORS = {
  green: "\e[92m",
  red: "\e[91m",
  yellow: "\e[93m",
  blue: "\e[94m",
  reset: "\e[0m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

# Caesar encrypt (ASCII letters only for simplicity)
def caesar_encrypt(text, shift)
  shift = shift % 26
  text.tr('A-Za-z', ('A'..'Z').to_a.rotate(shift).join + ('a'..'z').to_a.rotate(shift).join)
end

def caesar_decrypt(text, shift)
  caesar_encrypt(text, -shift)
end

def caesar_bruteforce(text)
  candidates = []
  (0...26).each do |s|
    dec = caesar_encrypt(text, -s)
    score = dec.count('etaoinshrdluETAOINSHRDLU')
    candidates << { shift: s, dec: dec, score: score }
  end
  candidates.sort_by! { |c| -c[:score] }
  candidates
end

# XOR (symmetrical)
def xor_cipher(text, key)
  raise "Key cannot be empty" if key.empty?
  key_bytes = key.bytes
  text.bytes.map.with_index { |b, i| (b ^ key_bytes[i % key_bytes.length]).chr }.join
end

def read_file(filename)
  File.read(filename, encoding: 'UTF-8')
rescue => e
  raise "Cannot read file: #{e.message}"
end

def write_file(filename, content)
  File.write(filename, content, encoding: 'UTF-8')
rescue => e
  raise "Cannot write file: #{e.message}"
end

options = {
  encrypt: false,
  decrypt: false,
  caesar: false,
  xor: false,
  key: nil,
  text: nil,
  input: nil,
  output: nil,
  bruteforce: false
}

OptionParser.new do |opts|
  opts.banner = "Usage: caesar_xor.rb [options]"
  opts.on('-e', '--encrypt', 'Encrypt mode (default)') { options[:encrypt] = true }
  opts.on('-d', '--decrypt', 'Decrypt mode') { options[:decrypt] = true }
  opts.on('-c', '--caesar', 'Use Caesar cipher') { options[:caesar] = true }
  opts.on('-x', '--xor', 'Use XOR cipher') { options[:xor] = true }
  opts.on('-k', '--key KEY', 'Key: shift (int) for Caesar or string for XOR') { |k| options[:key] = k }
  opts.on('-t', '--text TEXT', 'Input text') { |t| options[:text] = t }
  opts.on('-i', '--input FILE', 'Input file') { |f| options[:input] = f }
  opts.on('-o', '--output FILE', 'Output file') { |f| options[:output] = f }
  opts.on('-b', '--bruteforce', 'Bruteforce Caesar (ignores key)') { options[:bruteforce] = true }
  opts.on('-h', '--help', 'Show this help') do
    puts opts
    exit
  end
end.parse!

if options[:decrypt] && options[:encrypt]
  puts colorize("Error: cannot use both -e and -d", :red)
  exit 1
end
mode = options[:decrypt] ? 'decrypt' : 'encrypt'

if options[:caesar] && options[:xor]
  puts colorize("Error: choose only one cipher (-c or -x)", :red)
  exit 1
end
if !options[:caesar] && !options[:xor]
  puts colorize("Error: specify cipher (-c or -x)", :red)
  exit 1
end

# Read input
input_text = nil
if options[:text]
  input_text = options[:text]
elsif options[:input]
  begin
    input_text = read_file(options[:input])
  rescue => e
    puts colorize("Error reading input file: #{e}", :red)
    exit 1
  end
else
  # read from stdin
  if $stdin.tty?
    print colorize("Enter text (end with Ctrl+D): ", :yellow)
  end
  input_text = $stdin.read
  if input_text.nil? || input_text.empty?
    puts colorize("No input text", :red)
    exit 1
  end
end

result = nil
begin
  if options[:caesar]
    if options[:bruteforce]
      puts colorize("Bruteforce Caesar mode:", :blue)
      candidates = caesar_bruteforce(input_text)
      candidates.first(5).each do |c|
        puts "Shift #{c[:shift].to_s.rjust(2)} (score #{c[:score]}): #{colorize(c[:dec], :green)}"
      end
      best = candidates.first
      puts colorize("\nMost likely shift: #{best[:shift]}", :yellow)
      puts colorize("Result: #{best[:dec]}", :green)
      result = best[:dec]
    else
      shift = options[:key].to_i
      if mode == 'encrypt'
        result = caesar_encrypt(input_text, shift)
      else
        result = caesar_decrypt(input_text, shift)
      end
    end
  else # XOR
    raise "Key for XOR cannot be empty" if options[:key].nil? || options[:key].empty?
    result = xor_cipher(input_text, options[:key])
  end
rescue => e
  puts colorize("Error: #{e.message}", :red)
  exit 1
end

if options[:output]
  begin
    write_file(options[:output], result)
    puts colorize("Result written to #{options[:output]}", :green)
  rescue => e
    puts colorize("Error writing output: #{e}", :red)
    exit 1
  end
else
  print result
end
