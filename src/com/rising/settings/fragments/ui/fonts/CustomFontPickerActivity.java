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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.os.Handler;
import android.graphics.Typeface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.settings.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CustomFontPickerActivity extends AppCompatActivity {

    private static final String TAG = "CustomFontPicker";
    private static final int REQUEST_FONT_FILE = 1001;
    
    private FontManager mFontManager;
    private ListView mFontListView;
    private TextView mEmptyTextView;
    private Button mImportButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_font_picker);
        
        mFontManager = new FontManager(this, false);
        
        mFontListView = findViewById(R.id.custom_font_list);
        mEmptyTextView = findViewById(R.id.empty_text);
        mImportButton = findViewById(R.id.import_button);
        
        mImportButton.setOnClickListener(v -> openFilePicker());
        
        refreshFontList();
    }
    
    private void refreshFontList() {
        Log.d(TAG, "Refreshing font list");
        // Create a new FontManager instance to ensure we have the latest data
        mFontManager = new FontManager(this, false);
        List<String> fontNames = mFontManager.getCustomFontNames();
        
        Log.d(TAG, "Found " + fontNames.size() + " custom fonts");
        for (String font : fontNames) {
            Log.d(TAG, "Found font: " + font);
        }
        
        if (fontNames.isEmpty()) {
            mEmptyTextView.setVisibility(View.VISIBLE);
            mFontListView.setVisibility(View.GONE);
        } else {
            mEmptyTextView.setVisibility(View.GONE);
            mFontListView.setVisibility(View.VISIBLE);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_list_item_1, fontNames);
            mFontListView.setAdapter(adapter);
            
            mFontListView.setOnItemClickListener((parent, view, position, id) -> {
                String fontName = fontNames.get(position);
                showFontOptions(fontName);
            });
        }
    }
    
    private void showFontOptions(final String fontName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fontName)
               .setItems(new String[]{"Preview", "Set as system font", "Delete"}, 
                    (dialog, which) -> {
                        switch (which) {
                            case 0: // Preview
                                previewFont(fontName);
                                break;
                            case 1: // Set as system font
                                setAsSystemFont(fontName);
                                break;
                            case 2: // Delete
                                deleteFont(fontName);
                                break;
                        }
                    })
               .show();
    }
    
    private void previewFont(String fontName) {
        // Create a preview dialog
        File fontFile = new File(new File(getFilesDir(), "fonts"), fontName);
        
        try {
            Typeface typeface = Typeface.createFromFile(fontFile);
            
            // Create a custom view for the preview
            TextView previewText = new TextView(this);
            previewText.setPadding(50, 50, 50, 50);
            previewText.setTypeface(typeface);
            previewText.setTextSize(24);
            previewText.setText("The quick brown fox jumps over the lazy dog\n\n" +
                               "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
                               "abcdefghijklmnopqrstuvwxyz\n" +
                               "1234567890");
            
            new AlertDialog.Builder(this)
                .setTitle("Preview: " + fontName)
                .setView(previewText)
                .setPositiveButton("Close", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Error previewing font", e);
            Toast.makeText(this, "Could not preview font", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setAsSystemFont(String fontName) {
        // Add the implementation to set as system font
        Toast.makeText(this, "Setting " + fontName + " as system font", Toast.LENGTH_SHORT).show();
        
        // Get all font packages and find the index of the custom font package
        List<String> packages = mFontManager.getAllFontPackages();
        int customFontIndex = packages.indexOf(FontManager.CUSTOM_FONT_PKG);
        
        if (customFontIndex >= 0) {
            try {
                // Store the selected font name first to ensure it's used
                File fontFile = new File(new File(getFilesDir(), "fonts"), fontName);
                getSharedPreferences("font_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("custom_font_path", fontFile.getAbsolutePath())
                    .apply();
                
                // Enable the custom font
                mFontManager.enableFontPackage(customFontIndex);
                
                // Show restart dialog
                new AlertDialog.Builder(this)
                    .setTitle("Font Applied")
                    .setMessage("Your custom font has been applied. You need to restart your device for the changes to take full effect.")
                    .setPositiveButton("Restart Now", (dialog, which) -> {
                        // Request a system restart
                        try {
                            Intent intent = new Intent(Intent.ACTION_REBOOT);
                            intent.putExtra("nowait", 1);
                            intent.putExtra("interval", 1);
                            intent.putExtra("window", 0);
                            sendBroadcast(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error requesting reboot", e);
                            Toast.makeText(this, "Please restart your device manually", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Later", null)
                    .show();
            } catch (Exception e) {
                Log.e(TAG, "Error setting system font", e);
                Toast.makeText(this, "Error setting system font: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Custom font package not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteFont(String fontName) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Font")
            .setMessage("Are you sure you want to delete " + fontName + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Use the app's private file directory
                File fontFile = new File(new File(getFilesDir(), "fonts"), fontName);
                Log.d(TAG, "Deleting font at: " + fontFile.getAbsolutePath());
                
                if (fontFile.exists() && fontFile.delete()) {
                    Toast.makeText(this, "Font deleted", Toast.LENGTH_SHORT).show();
                    refreshFontList();
                } else {
                    Toast.makeText(this, "Failed to delete font", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete failed. File exists: " + fontFile.exists() + 
                        ", Can write: " + fontFile.canWrite());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        
        // Only show TTF and OTF files
        String[] mimeTypes = {"font/ttf", "font/otf", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        startActivityForResult(intent, REQUEST_FONT_FILE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_FONT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                importFont(uri);
            }
        }
    }
    
    private void importFont(Uri uri) {
        try {
            // Create a temporary file
            File tempFile = File.createTempFile("font", ".tmp", getCacheDir());
            tempFile.deleteOnExit();
            
            Log.d(TAG, "Created temp file: " + tempFile.getAbsolutePath());
            
            // Copy the content from the URI to the temporary file
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            
            Log.d(TAG, "Content copied to temp file, importing now");
            
            // Import the font
            boolean result = mFontManager.importCustomFont(tempFile.getAbsolutePath());
            Log.d(TAG, "Import result: " + result);
            
            if (result) {
                Toast.makeText(this, "Font imported successfully", Toast.LENGTH_SHORT).show();
                // Add a delay before refresh to ensure file operations complete
                new Handler().postDelayed(this::refreshFontList, 500);
            } else {
                Toast.makeText(this, "Failed to import font", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error importing font", e);
            Toast.makeText(this, "Error importing font: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
