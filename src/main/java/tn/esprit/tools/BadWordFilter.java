package tn.esprit.tools;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class to filter prohibited words in posts and comments.
 */
public class BadWordFilter {
    
    // A sample list of common prohibited words (French & English)
    private static final List<String> PROHIBITED_WORDS = Arrays.asList(
        "merde", "putain", "con", "salaud", "idiot", "stupid", "fuck", "shit", "ass", "bastard"
    );

    /**
     * Checks if a text contains any prohibited words.
     * Use whole-word matching to avoid false positives (e.g., "class" contains "ass").
     * 
     * @param text The string to check.
     * @return true if a prohibited word is found, false otherwise.
     */
    public static boolean containsBadWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        for (String word : PROHIBITED_WORDS) {
            // Regex \b matches word boundaries
            String regex = "\\b" + Pattern.quote(word) + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
