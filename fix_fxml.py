import os
import re

def fix_fxml_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove literal \n characters that were added by mistake
    content = content.replace('`n', '\n')
    
    # Remove duplicate Region elements
    # Pattern: Region followed by closing tags, then another Region comment and element
    content = re.sub(
        r'(<Region prefHeight="150"\s*/>\s*</(?:VBox|HBox)>)\s*<!--[^>]*EXTRA BOTTOM SPACE[^>]*-->\s*<Region prefHeight="150"\s*/>',
        r'\1',
        content,
        flags=re.DOTALL
    )
    
    # Fix cases where Region is outside the main container
    # Pattern 1: Region after </center> - should be inside
    if '</center>' in content and '<!-- EXTRA BOTTOM SPACE' in content:
        # Find if Region is after </center>
        if re.search(r'</center>\s*<!--[^>]*EXTRA BOTTOM SPACE', content):
            # Move it before the closing VBox/ScrollPane
            content = re.sub(
                r'(</(?:VBox|HBox)>\s*</ScrollPane>\s*</center>)\s*<!--[^>]*EXTRA BOTTOM SPACE[^>]*-->\s*<Region prefHeight="150"\s*/>',
                r'<!-- EXTRA BOTTOM SPACE FOR SCROLLING -->\n                <Region prefHeight="150" />\n\n            \1',
                content
            )
    
    # Pattern 2: Region after </bottom> - should be inside
    if '</bottom>' in content and '<!-- EXTRA BOTTOM SPACE' in content:
        if re.search(r'</bottom>\s*<!--[^>]*EXTRA BOTTOM SPACE', content):
            content = re.sub(
                r'(</(?:VBox|HBox)>\s*</bottom>)\s*<!--[^>]*EXTRA BOTTOM SPACE[^>]*-->\s*<Region prefHeight="150"\s*/>',
                r'<!-- EXTRA BOTTOM SPACE FOR SCROLLING -->\n                <Region prefHeight="150" />\n\n            \1',
                content
            )
    
    # Pattern 3: Region after </content> - should be inside
    if '</content>' in content and '<!-- EXTRA BOTTOM SPACE' in content:
        if re.search(r'</content>\s*<!--[^>]*EXTRA BOTTOM SPACE', content):
            content = re.sub(
                r'(</(?:VBox|HBox)>\s*</content>)\s*<!--[^>]*EXTRA BOTTOM SPACE[^>]*-->\s*<Region prefHeight="150"\s*/>',
                r'<!-- EXTRA BOTTOM SPACE FOR SCROLLING -->\n                <Region prefHeight="150" />\n\n            \1',
                content
            )
    
    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    
    return True

# Find all FXML files
fxml_files = []
for root, dirs, files in os.walk('src/main/resources/views'):
    for file in files:
        if file.endswith('.fxml'):
            fxml_files.append(os.path.join(root, file))

print(f"Found {len(fxml_files)} FXML files")

for fxml_file in fxml_files:
    try:
        fix_fxml_file(fxml_file)
        print(f"Fixed: {fxml_file}")
    except Exception as e:
        print(f"Error fixing {fxml_file}: {e}")

print("Done!")
