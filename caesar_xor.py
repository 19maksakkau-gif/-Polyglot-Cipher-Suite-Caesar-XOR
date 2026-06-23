# caesar_xor.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import argparse
import os
import re
from collections import Counter

# ANSI color codes
COLOR = {
    'green': '\033[92m',
    'red': '\033[91m',
    'yellow': '\033[93m',
    'blue': '\033[94m',
    'reset': '\033[0m'
}

def colorize(text, color):
    return f"{COLOR.get(color, '')}{text}{COLOR['reset']}"

def caesar_encrypt(text, shift):
    """Шифрование Цезаря с поддержкой Unicode (буквы только)."""
    result = []
    for ch in text:
        if ch.isalpha():
            base = ord('A') if ch.isupper() else ord('a')
            # Для кириллицы используем отдельные диапазоны
            if 'А' <= ch <= 'Я' or 'а' <= ch <= 'я':
                base = ord('А') if ch.isupper() else ord('а')
                alphabet_size = 32  # русский алфавит без Ё
            else:
                alphabet_size = 26
            result.append(chr((ord(ch) - base + shift) % alphabet_size + base))
        else:
            result.append(ch)
    return ''.join(result)

def caesar_decrypt(text, shift):
    return caesar_encrypt(text, -shift)

def caesar_bruteforce(text):
    """Брутфорс всех сдвигов (только латиница для частотного анализа)."""
    candidates = []
    for shift in range(26):
        dec = caesar_encrypt(text, -shift)
        # Простейший частотный анализ: подсчет пробелов и частотных букв
        score = sum(1 for ch in dec.lower() if ch in 'etaoinshrdlu')
        candidates.append((shift, dec, score))
    # Сортируем по убыванию score
    candidates.sort(key=lambda x: x[2], reverse=True)
    return candidates

def xor_encrypt(text, key):
    """XOR шифрование/дешифрование (симметрично)."""
    if not key:
        raise ValueError("Ключ не может быть пустым")
    key_bytes = key.encode('utf-8')
    text_bytes = text.encode('utf-8')
    result = bytearray()
    for i, b in enumerate(text_bytes):
        result.append(b ^ key_bytes[i % len(key_bytes)])
    return result.decode('utf-8', errors='replace')

def xor_decrypt(text, key):
    return xor_encrypt(text, key)  # симметрично

def read_text_from_file(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        return f.read()

def write_text_to_file(filename, content):
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

def main():
    parser = argparse.ArgumentParser(description="Caesar & XOR Cipher Tool")
    parser.add_argument('-e', '--encrypt', action='store_true', help='Encrypt mode (default)')
    parser.add_argument('-d', '--decrypt', action='store_true', help='Decrypt mode')
    parser.add_argument('-c', '--caesar', action='store_true', help='Use Caesar cipher')
    parser.add_argument('-x', '--xor', action='store_true', help='Use XOR cipher')
    parser.add_argument('-k', '--key', required=True, help='Key: shift (int) for Caesar or string for XOR')
    parser.add_argument('-t', '--text', help='Input text')
    parser.add_argument('-i', '--input', help='Input file')
    parser.add_argument('-o', '--output', help='Output file')
    parser.add_argument('-b', '--bruteforce', action='store_true', help='Bruteforce Caesar (ignores key)')

    args = parser.parse_args()

    # Определяем режим
    if args.decrypt and args.encrypt:
        print(colorize("Ошибка: нельзя указать и -e, и -d одновременно", 'red'))
        sys.exit(1)
    mode = 'decrypt' if args.decrypt else 'encrypt'

    if args.caesar and args.xor:
        print(colorize("Ошибка: выберите только один шифр (-c или -x)", 'red'))
        sys.exit(1)
    if not args.caesar and not args.xor:
        print(colorize("Ошибка: укажите шифр (-c или -x)", 'red'))
        sys.exit(1)

    # Чтение текста
    if args.text:
        text = args.text
    elif args.input:
        try:
            text = read_text_from_file(args.input)
        except Exception as e:
            print(colorize(f"Ошибка чтения файла: {e}", 'red'))
            sys.exit(1)
    else:
        # Чтение из stdin
        if sys.stdin.isatty():
            print(colorize("Введите текст (завершите Ctrl+D или Ctrl+Z):", 'yellow'))
        text = sys.stdin.read()

    if not text:
        print(colorize("Ошибка: текст не введен", 'red'))
        sys.exit(1)

    # Обработка
    try:
        if args.caesar:
            if args.bruteforce:
                print(colorize("Режим брутфорса Цезаря:", 'blue'))
                candidates = caesar_bruteforce(text)
                # Показываем топ-5
                for shift, dec, score in candidates[:5]:
                    print(f"Сдвиг {shift:2d} (score {score}): {colorize(dec, 'green')}")
                # Лучший вариант
                best_shift, best_dec, _ = candidates[0]
                print(colorize(f"\nНаиболее вероятный сдвиг: {best_shift}", 'yellow'))
                print(colorize(f"Результат: {best_dec}", 'green'))
                result = best_dec
            else:
                try:
                    shift = int(args.key)
                except ValueError:
                    print(colorize("Ключ для Цезаря должен быть целым числом", 'red'))
                    sys.exit(1)
                if mode == 'encrypt':
                    result = caesar_encrypt(text, shift)
                else:
                    result = caesar_decrypt(text, shift)
        else:  # XOR
            key = args.key
            if mode == 'encrypt':
                result = xor_encrypt(text, key)
            else:
                result = xor_decrypt(text, key)
    except Exception as e:
        print(colorize(f"Ошибка: {e}", 'red'))
        sys.exit(1)

    # Вывод
    if args.output:
        try:
            write_text_to_file(args.output, result)
            print(colorize(f"Результат записан в {args.output}", 'green'))
        except Exception as e:
            print(colorize(f"Ошибка записи файла: {e}", 'red'))
            sys.exit(1)
    else:
        print(result)

if __name__ == '__main__':
    main()
