// Validate the server-side language files against the English fallback.
const fs = require('node:fs');
const path = require('node:path');

const directory = process.argv[2] || 'src/main/resources/assets/teleportgui/lang';
const fallbackFile = 'en_us.json';
const placeholders = text => new Set(text.match(/%\d+%/g) || []);
let errors = 0;

function error(message) {
  console.error(`ERROR: ${message}`);
  errors++;
}

function readLanguage(file) {
  try {
    const data = JSON.parse(fs.readFileSync(path.join(directory, file), 'utf8'));
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      throw new Error('expected a JSON object');
    }
    for (const [key, value] of Object.entries(data)) {
      if (typeof value !== 'string') throw new Error(`value for ${key} must be a string`);
      for (const placeholder of placeholders(value)) {
        if (BigInt(placeholder.slice(1, -1)) > 2147483647n) {
          throw new Error(`placeholder ${placeholder} in ${key} exceeds the supported index range`);
        }
      }
    }
    return data;
  } catch (exception) {
    error(`${file}: ${exception.message}`);
    return null;
  }
}

let files;
try {
  files = fs.readdirSync(directory).filter(file => file.endsWith('.json')).sort();
} catch (exception) {
  error(`${directory}: ${exception.message}`);
  process.exit(1);
}
const fallback = readLanguage(fallbackFile);
if (fallback) {
  for (const file of files.filter(file => file !== fallbackFile)) {
    const translations = readLanguage(file);
    if (!translations) continue;
    for (const [key, original] of Object.entries(fallback)) {
      if (!Object.hasOwn(translations, key)) {
        console.warn(`WARNING: ${file}: missing ${key}; English fallback will be used`);
        continue;
      }
      const expected = [...placeholders(original)].sort().join(', ');
      const actual = [...placeholders(translations[key])].sort().join(', ');
      if (expected !== actual) error(`${file}: ${key}: expected [${expected}], got [${actual}]`);
    }
    for (const key of Object.keys(translations)) {
      if (!Object.hasOwn(fallback, key)) console.warn(`WARNING: ${file}: ${key} is absent from ${fallbackFile}`);
    }
  }
}
if (errors) process.exitCode = 1;
else console.log(`Validated ${files.length} language files`);
