/*
 * Copyright (C) 2023 The risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments.ui.fonts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.om.OverlayInfo;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.android.internal.util.android.ThemeUtils;

public class FontManager {
    
    private static final String TAG = "FontManager";
    private final static String DEFAULT_FONT_PACKAGE = "android";
    private final static String FONT_OVERLAY_CATEGORY = "android.theme.customization.font";
    private final static String LOCKSCREEN_FONT_OVERLAY_CATEGORY = "android.theme.customization.lockscreen_clock_font"; 
    private final static String THEME_RESOURCE_FONT_FAMILY = "config_bodyFontFamily";
    private final static String THEME_RESOURCE_CLOCK_FONT_FAMILY = "config_clockFontFamily";
    private static final String THEME_RESOURCE_HEADLINE_FONT_FAMILY = "config_headlineFontFamily";
    
    // Changed to use app-specific storage instead of system directory
    private static String CUSTOM_FONT_DIR;
    public static final String CUSTOM_FONT_PKG = "custom.font.package";
    
    private static final Set<String> HEADLINE_FONT_LABEL_MAP = new HashSet<>();

    private ThemeUtils mThemeUtils;
    private boolean isLockscreen;
    private Context mContext;

    static {
        HEADLINE_FONT_LABEL_MAP.add("NothingDot57");
    }

    public FontManager(Context context, boolean lockscreen) {
        mThemeUtils = ThemeUtils.getInstance(context);
        isLockscreen = lockscreen;
        mContext = context;
        
        // Initialize the font directory path using app context
        CUSTOM_FONT_DIR = new File(context.getFilesDir(), "fonts").getAbsolutePath() + "/";
        Log.d(TAG, "Font directory set to: " + CUSTOM_FONT_DIR);
        
        // Ensure custom font directory exists
        createCustomFontDirectory();
    }

    /**
     * Create the directory for custom fonts if it doesn't exist
     */
    private void createCustomFontDirectory() {
        File fontDir = new File(CUSTOM_FONT_DIR);
        if (!fontDir.exists()) {
            if (fontDir.mkdirs()) {
                Log.d(TAG, "Custom font directory created: " + CUSTOM_FONT_DIR);
            } else {
                Log.e(TAG, "Failed to create custom font directory: " + CUSTOM_FONT_DIR);
            }
        }
    }

    /**
     * Get all available fonts and return as a list of typefaces.
     */
    public List<Typeface> getFonts() {
        List<Typeface> fonts = mThemeUtils.getFonts();
        
        // Add custom fonts if available
        File fontDir = new File(CUSTOM_FONT_DIR);
        if (fontDir.exists() && fontDir.isDirectory()) {
            File[] fontFiles = fontDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
            
            if (fontFiles != null) {
                for (File font : fontFiles) {
                    try {
                        Typeface typeface = Typeface.createFromFile(font);
                        if (typeface != null) {
                            fonts.add(typeface);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading custom font: " + font.getName(), e);
                    }
                }
            }
        }
        
        return fonts;
    }

    /**
     * Get all available font packages.
     */
    public List<String> getAllFontPackages() {
        List<String> packages = mThemeUtils.getOverlayPackagesForCategory(
            isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY, 
            DEFAULT_FONT_PACKAGE);
        
        // Add custom font option if we have custom fonts
        if (hasCustomFonts()) {
            packages.add(CUSTOM_FONT_PKG);
        }
        
        return packages;
    }
    
    /**
     * Check if custom fonts are available
     */
    public boolean hasCustomFonts() {
        File fontDir = new File(CUSTOM_FONT_DIR);
        if (fontDir.exists() && fontDir.isDirectory()) {
            File[] fontFiles = fontDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
            return fontFiles != null && fontFiles.length > 0;
        }
        return false;
    }
    
    /**
     * Get list of custom font file names
     */
    public List<String> getCustomFontNames() {
        List<String> fontNames = new ArrayList<>();
        
        Log.d(TAG, "Looking for fonts in directory: " + CUSTOM_FONT_DIR);
        File fontDir = new File(CUSTOM_FONT_DIR);
        Log.d(TAG, "Font directory exists: " + fontDir.exists());
        Log.d(TAG, "Font directory is directory: " + fontDir.isDirectory());
        
        if (fontDir.exists() && fontDir.isDirectory()) {
            File[] fontFiles = fontDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
            
            if (fontFiles != null) {
                Log.d(TAG, "Found " + fontFiles.length + " font files");
                for (File font : fontFiles) {
                    fontNames.add(font.getName());
                    Log.d(TAG, "Added font: " + font.getName());
                }
            } else {
                Log.d(TAG, "No font files found or listFiles returned null");
            }
        }
        
        return fontNames;
    }
    
    /**
     * Import a font file to the custom font directory
     */
    public boolean importCustomFont(String sourcePath) {
        FileInputStream in = null;
        FileOutputStream out = null;
        
        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                Log.e(TAG, "Source file doesn't exist or can't be read: " + sourcePath);
                return false;
            }
            
            // Ensure directory exists
            createCustomFontDirectory();
            
            String fileName = sourceFile.getName();
            // Ensure we have a valid font extension
            if (!fileName.toLowerCase().endsWith(".ttf") && !fileName.toLowerCase().endsWith(".otf")) {
                // Try to detect file type and add appropriate extension
                try {
                    FileInputStream is = new FileInputStream(sourceFile);
                    byte[] magic = new byte[4];
                    is.read(magic);
                    is.close();
                    
                    // Check magic numbers for TTF/OTF
                    if (magic[0] == 0x00 && magic[1] == 0x01 && magic[2] == 0x00 && magic[3] == 0x00) {
                        fileName += ".ttf";
                    } else if (magic[0] == 'O' && magic[1] == 'T' && magic[2] == 'T' && magic[3] == 'O') {
                        fileName += ".otf";
                    } else {
                        // Default to TTF
                        fileName += ".ttf";
                    }
                    Log.d(TAG, "Added extension to font file: " + fileName);
                } catch (IOException e) {
                    Log.e(TAG, "Error detecting font type", e);
                    fileName += ".ttf"; // Default extension
                }
            }
            
            File destFile = new File(CUSTOM_FONT_DIR, fileName);
            
            // Create parent directories if they don't exist
            destFile.getParentFile().mkdirs();
            
            Log.d(TAG, "Importing font from " + sourcePath + " to " + destFile.getAbsolutePath());
            
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            // Make sure the file is readable
            destFile.setReadable(true, false);
            Log.d(TAG, "File readable: " + destFile.canRead());
            
            // Create or update the font overlay
            createOrUpdateFontXml(destFile.getAbsolutePath());
            
            Log.d(TAG, "Successfully imported font to: " + destFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error importing custom font", e);
            return false;
        } finally {
            // Ensure streams are closed
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * Create or update the XML required for a font overlay
     */
    private void createOrUpdateFontXml(String fontPath) {
        try {
            // Store the font path in preferences
            mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("custom_font_path", fontPath)
                .apply();
            
            Log.d(TAG, "Font XML update prepared for: " + fontPath);
        } catch (Exception e) {
            Log.e(TAG, "Error creating font XML", e);
        }
    }
    
    /**
     * Create or update the font overlay for custom fonts
     */
    private void createOrUpdateFontOverlay() {
        try {
            // Get the selected custom font path
            String fontPath = mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
                .getString("custom_font_path", "");
            
            if (fontPath.isEmpty()) {
                Log.e(TAG, "No custom font path found");
                return;
            }
            
            // Create a custom typeface from the font file
            Typeface customTypeface = Typeface.createFromFile(new File(fontPath));
            if (customTypeface == null) {
                Log.e(TAG, "Could not create typeface from: " + fontPath);
                return;
            }
            
            // Store the path and overlay update flag in preferences
            mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("overlay_update_needed", true)
                .putString("custom_font_path", fontPath)
                .apply();
            
            // Update settings to mark custom font as active
            try {
                mThemeUtils.writeSettings(
                    isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY,
                    CUSTOM_FONT_PKG,
                    false);
                Log.d(TAG, "Font settings updated in secure settings");
            } catch (Exception e) {
                Log.e(TAG, "Error updating font settings", e);
            }
            
            Log.d(TAG, "Font overlay update completed for: " + fontPath);
            
            // Request a UI refresh
            mContext.sendBroadcast(new Intent(Intent.ACTION_CONFIGURATION_CHANGED));
        } catch (Exception e) {
            Log.e(TAG, "Error updating font overlay", e);
        }
    }

    /**
     * Get the currently selected font package.
     */
    public String getCurrentFontPackage() {
        List<OverlayInfo> overlayInfos = mThemeUtils.getOverlayInfos(
            isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY);
        
        String current = overlayInfos.stream()
                .filter(OverlayInfo::isEnabled)
                .map(OverlayInfo::getPackageName)
                .findFirst()
                .orElse(DEFAULT_FONT_PACKAGE);
        
        // Check if we're using a custom font
        if (current.equals(DEFAULT_FONT_PACKAGE) && isCustomFontEnabled()) {
            return CUSTOM_FONT_PKG;
        }
        
        return current;
    }
    
    /**
     * Check if a custom font is currently enabled
     */
    private boolean isCustomFontEnabled() {
        return mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
            .getBoolean("custom_font_enabled", false);
    }

    /**
     * Enable a selected font package.
     */
    public void enableFontPackage(int position) {
        if (position < 0 || position >= getAllFontPackages().size()) {
            throw new IllegalArgumentException("Invalid font package position: " + position);
        }
        
        String selectedPackage = getAllFontPackages().get(position);
        
        if (CUSTOM_FONT_PKG.equals(selectedPackage)) {
            // Enable custom font
            enableCustomFont();
        } else {
            // First, disable the custom font if it's enabled
            if (isCustomFontEnabled()) {
                mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("custom_font_enabled", false)
                    .apply();
            }
            
            // Enable system font package
            mThemeUtils.setOverlayEnabled(
                isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY, 
                selectedPackage, 
                DEFAULT_FONT_PACKAGE);
        }
    }
    
    /**
     * Enable the custom font
     */
    private void enableCustomFont() {
        List<String> customFonts = getCustomFontNames();
        if (customFonts.isEmpty()) {
            Log.e(TAG, "No custom fonts available to enable");
            return;
        }
        
        try {
            // First, disable any existing font overlays to avoid conflicts
            List<OverlayInfo> overlayInfos = mThemeUtils.getOverlayInfos(
                isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY);
            
            for (OverlayInfo info : overlayInfos) {
                if (info.isEnabled()) {
                    mThemeUtils.setOverlayEnabled(
                        isLockscreen ? LOCKSCREEN_FONT_OVERLAY_CATEGORY : FONT_OVERLAY_CATEGORY, 
                        DEFAULT_FONT_PACKAGE, 
                        info.packageName);
                    Log.d(TAG, "Disabled overlay: " + info.packageName);
                    break;
                }
            }
            
            // Get the path of the custom font
            String fontPath = CUSTOM_FONT_DIR + customFonts.get(0);
            
            // Log the selected font
            Log.d(TAG, "Custom font enabled: " + fontPath);
            
            // Store the preference that custom font is enabled
            mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("custom_font_enabled", true)
                .putString("custom_font_path", fontPath)
                .apply();
            
            // Create and apply the font overlay
            createOrUpdateFontOverlay();
        } catch (Exception e) {
            Log.e(TAG, "Error enabling custom font", e);
        }
    }

    /**
     * Gets the font package label.
     */
    public String getLabel(Context context, String pkg) {
        if (CUSTOM_FONT_PKG.equals(pkg)) {
            return "Custom Font";
        }
        
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {}
        return pkg;
    }

    /**
     * Gets the font package typeface.
     */
    public Typeface getTypeface(Context context, String pkg) {
        if (CUSTOM_FONT_PKG.equals(pkg)) {
            return getCustomTypeface();
        }
        
        PackageManager pm = context.getPackageManager();
        try {
            Resources res = pkg.equals(DEFAULT_FONT_PACKAGE) ? Resources.getSystem()
                    : pm.getResourcesForApplication(pkg);
            String label = getLabel(context, pkg);
            String identifier = isLockscreen ? THEME_RESOURCE_CLOCK_FONT_FAMILY : THEME_RESOURCE_FONT_FAMILY;
            if (!isLockscreen && HEADLINE_FONT_LABEL_MAP.contains(label)) {
                identifier = THEME_RESOURCE_HEADLINE_FONT_FAMILY;
            }
            return Typeface.create(res.getString(
                    res.getIdentifier(identifier, "string", pkg)), Typeface.NORMAL);
        } catch (PackageManager.NameNotFoundException e) {}
        return null;
    }
    
    /**
     * Get the custom typeface
     */
    private Typeface getCustomTypeface() {
        // First try to get path from preferences
        String fontPath = mContext.getSharedPreferences("font_prefs", Context.MODE_PRIVATE)
            .getString("custom_font_path", "");
            
        if (!fontPath.isEmpty()) {
            try {
                File fontFile = new File(fontPath);
                if (fontFile.exists() && fontFile.canRead()) {
                    return Typeface.createFromFile(fontFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading custom typeface from path: " + fontPath, e);
            }
        }
            
        // Fallback: get the first custom font file
        List<String> customFonts = getCustomFontNames();
        if (!customFonts.isEmpty()) {
            try {
                File fontFile = new File(CUSTOM_FONT_DIR, customFonts.get(0));
                return Typeface.createFromFile(fontFile);
            } catch (Exception e) {
                Log.e(TAG, "Error loading custom typeface", e);
            }
        }
        return null;
    }
}
