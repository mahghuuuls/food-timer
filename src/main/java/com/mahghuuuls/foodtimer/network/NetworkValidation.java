package com.mahghuuuls.foodtimer.network;

import net.minecraft.util.ResourceLocation;

/**
 * Strict wire validation missing from Minecraft 1.12.2's permissive ResourceLocation constructor.
 */
final class NetworkValidation {

    private NetworkValidation() {
    }

    static boolean isCanonicalRegistryName(String value) {
        if (value == null) {
            return false;
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            return false;
        }
        if (!hasOnlyNamespaceCharacters(value, 0, separator)
                || !hasOnlyPathCharacters(value, separator + 1, value.length())) {
            return false;
        }
        return new ResourceLocation(value).toString().equals(value);
    }

    private static boolean hasOnlyNamespaceCharacters(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            if (!isLowercaseLetterOrDigit(character)
                    && character != '_'
                    && character != '-'
                    && character != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean hasOnlyPathCharacters(String value, int start, int end) {
        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            if (!isLowercaseLetterOrDigit(character)
                    && character != '_'
                    && character != '-'
                    && character != '.'
                    && character != '/') {
                return false;
            }
        }
        return true;
    }

    private static boolean isLowercaseLetterOrDigit(char character) {
        return (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9');
    }
}
