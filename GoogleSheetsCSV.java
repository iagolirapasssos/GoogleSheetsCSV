/*
 * The MIT License (MIT)
Copyright © 2023 <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
documentation files (the “Software”), to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
IN THE SOFTWARE.
 */

/*
 * Extension for Google Sheets (CSV)
 * Version: 1.0
 * Author: Francisco Iago Lira Passos
 * Date: 2023-11-02
 */

package io.googlesheetscsv;

import android.content.Context;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.annotations.SimpleEvent;

import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.YailDictionary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

@DesignerComponent(
        version = 1,
        description = "Extension for Google Sheets (CSV)",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
public class GoogleSheetsCSV extends AndroidNonvisibleComponent {

    private Context context;
    private String csvUrl;
    private List<String> csvData = new ArrayList<>();

    public GoogleSheetsCSV(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
    }

    @SimpleProperty(description = "Set the CSV URL")
    public void CsvUrl(String csvUrl) {
        this.csvUrl = csvUrl;
    }
    

    @SimpleFunction(description = "Read data from the specified CSV URL and send it as an event. "
            + "The CSV URL must be set using the CsvUrl property before calling this function.\n"
            + "When the data is successfully retrieved, the DataRead event will be triggered.\n"
            + "In case of an error, the ErrorOccurred event will be triggered.\n"
            + "Note: Ensure that you set the CsvUrl property with a valid URL before calling this function.")
    public void ReadDataFromCSVUrl() {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    //Log.i("GoogleSheetsCSV", "Reading data from CSV URL...");
                    List<String> csvData = readDataFromCSVUrl();
                    if (csvData.isEmpty()) {
                        //Log.e("GoogleSheetsCSV", "CSV data is empty.");
                        ErrorOccurred("CSV data is empty.");
                        return;
                    }
                    final YailList dataList = YailList.makeList(csvData);
                    form.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DataRead(dataList);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    //Log.e("GoogleSheetsCSV", "Error reading data from CSV URL: " + e.getMessage());
                    ErrorOccurred("Error reading data from CSV URL: " + e.getMessage());
                }
            }
        });
    }
    
    private List<String> readDataFromCSVUrl() throws Exception {
        List<String> csvData = new ArrayList<>();
        URL url = new URL(csvUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            csvData.add(line);
        }
        reader.close();
        connection.disconnect();
        return csvData;
    }


    @SimpleFunction(description = "Get a value from the CSV data by specifying the row and column indices. "
            + "Returns the value at the given row and column indices. Returns an empty string if the row or column is out of bounds.\n"
            + "Parameters:\n"
            + "- rowIndex (int): The index of the row (1-based index).\n"
            + "- columnIndex (int): The index of the column (1-based index).")
    public String GetValueByRowAndColumn(int rowIndex, int columnIndex, YailList data) {
        if (rowIndex < 1 || rowIndex > data.toStringArray().length || columnIndex < 1) {
            return "";  // Linha ou coluna inválida
        }

        String[] rows = data.toStringArray();
        String[] columns = rows[rowIndex - 1].split(",");

        if (columnIndex <= columns.length) {
            return columns[columnIndex - 1].trim();
        } else {
            return "";  // Coluna inválida
        }
    }
    
    @SimpleFunction(description = "Read data from a CSV file and send it as an event.")
    public void ReadCSVFile(String filePath) {
        try {
            //Log.i("GoogleSheetsCSV", "Reading data from CSV file...");
            List<String> csvData = readDataFromCSVFile(filePath);
            final YailList dataList = YailList.makeList(csvData);
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DataRead(dataList);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            //Log.e("GoogleSheetsCSV", "Error reading data from CSV file: " + e.getMessage());
            ErrorOccurred("Error reading data from CSV file: " + e.getMessage());
        }
    }

    private List<String> readDataFromCSVFile(String filePath) throws Exception {
        List<String> csvData = new ArrayList<>();
        File csvFile = new File(filePath);
        if (csvFile.exists()) {
            FileReader fileReader = new FileReader(csvFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            while ((line = reader.readLine()) != null) {
                csvData.add(line);
            }
            reader.close();
            fileReader.close();
        } else {
            throw new Exception("CSV file not found.");
        }
        return csvData;
    }

    @SimpleFunction(description = "Writes or overwrites data to a CSV file.\n\n"
            + "This function prepares the data for a CSV file, including headers and company data, and writes it to the specified file path. If the file already exists, its contents will be overwritten. If the file does not exist, it will be created.\n\n"
            + "Parameters:\n"
            + "- data (List): The data to be written to the CSV file. Each element in the list represents a row in the CSV file, and elements should be comma-separated values.\n"
            + "- filePath (String): The file path where the CSV file will be created or overwritten.\n\n"
            + "Note: Ensure that you have the necessary write permissions before calling this function. If not, it will request the necessary permission."
    )
    public void WriteCSVFile(YailList data, String filePath) {
        if (isWritePermissionGranted()) {
            try {
                File csvFile = new File(filePath);

                if (!csvFile.exists()) {
                    // Se o arquivo não existir, crie-o
                    csvFile.createNewFile();
                }

                FileWriter fileWriter = new FileWriter(csvFile, false); // Sobrescrever o conteúdo

                // Adicionar os dados fornecidos diretamente ao arquivo
                for (String rowData : data.toStringArray()) {
                    fileWriter.write(rowData + "\n");
                }

                fileWriter.close();
                //Log.i("GoogleSheetsCSV", "Data written to CSV file");
            } catch (IOException e) {
                e.printStackTrace();
                //Log.e("GoogleSheetsCSV", "Error writing data to CSV file: " + e.getMessage());
                ErrorOccurred("Error writing data to CSV file: " + e.getMessage());
            }
        } else {
            // Request write permission
            ActivityCompat.requestPermissions(form, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    
    private boolean isWritePermissionGranted() {
        int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return writePermission == PackageManager.PERMISSION_GRANTED;
    }

    
    private String escapeForCSV(String input) {
        if (input.contains(",") || input.contains("\n") || input.contains("\"")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        } else {
            return input;
        }
    }

    @SimpleEvent(description = "Triggered after successfully reading data from the CSV URL. "
    	    + "This event is triggered when the data from the CSV URL is successfully read and retrieved. "
    	    + "It provides the retrieved data as a List parameter.")
    public void DataRead(YailList data) {
        EventDispatcher.dispatchEvent(this, "DataRead", data);
    }

    @SimpleEvent(description = "Triggered after an error occurs during the extension's operations. "
    	    + "This event is triggered when an error occurs during the extension's operations. It provides an error message as a parameter.")
    public void ErrorOccurred(String error) {
        EventDispatcher.dispatchEvent(this, "ErrorOccurred", error);
    }
}
