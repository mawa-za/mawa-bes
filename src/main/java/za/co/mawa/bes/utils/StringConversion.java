package za.co.mawa.bes.utils;

public class StringConversion {
    public static String capitalizeFully(String input) {
        if (input == null) {
            return null;
        }

        String s = input.toLowerCase();

        int strLen = s.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = s.charAt(i);

            if (Character.isWhitespace(ch)) {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }
}
