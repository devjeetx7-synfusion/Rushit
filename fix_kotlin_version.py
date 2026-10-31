import re

with open('build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('version "1.9.24"', 'version "1.9.22"')

with open('build.gradle.kts', 'w') as f:
    f.write(content)
