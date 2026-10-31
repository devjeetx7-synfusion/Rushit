import re

# Kotlin 1.9.22 needs Compose Compiler 1.5.9
with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('kotlinCompilerExtensionVersion = "1.5.14"', 'kotlinCompilerExtensionVersion = "1.5.9"')

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

with open('admin/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('kotlinCompilerExtensionVersion = "1.5.14"', 'kotlinCompilerExtensionVersion = "1.5.9"')

with open('admin/build.gradle.kts', 'w') as f:
    f.write(content)
