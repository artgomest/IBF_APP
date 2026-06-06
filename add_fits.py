import os
import re

directories = ['app/src/main/res/layout', 'app/src/main/res/layout-night']

for directory in directories:
    if not os.path.exists(directory):
        continue
    for filename in os.listdir(directory):
        if filename.startswith('activity_') and filename.endswith('.xml'):
            filepath = os.path.join(directory, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'fitsSystemWindows="true"' not in content:
                # Look for xmlns:android
                pattern = r'(<[a-zA-Z0-9\._]+[\s\n]+xmlns:android="http://schemas.android.com/apk/res/android")'
                new_content, count = re.subn(pattern, r'\1\n    android:fitsSystemWindows="true"', content, count=1)
                
                if count == 0:
                    print(f"Could not find root element with xmlns in {filename}")
                else:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f'Added to {filepath}')
