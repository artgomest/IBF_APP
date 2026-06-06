import re
with open('app/src/main/java/com/ibf/app/ui/usuarios/FichaMembroActivity.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    cleaned = re.sub(r'\"(?:\\\\\"|[^\"])*?\"', '\"\"', line)
    cleaned = re.sub(r'//.*', '', cleaned)
    for char in cleaned:
        if char == '{': depth += 1
        elif char == '}': depth -= 1
    if line.strip().startswith('private fun '):
        print(f'{i+1:04d}: depth {depth}: {line.strip()}')
