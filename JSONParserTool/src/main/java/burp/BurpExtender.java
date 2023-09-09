package burp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.event.ListSelectionListener;
import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BurpExtender implements IBurpExtender, ITab {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JPanel mainPanel;
    public PrintWriter stdout;
    private JTextField fieldNameTextField;
    private JButton extractButton;
    private JTextArea outputTextArea;
    private JTextArea jsonInputTextArea;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.helpers = callbacks.getHelpers();
        this.stdout.println("######################");
        this.stdout.println("[+] code by dou");
        this.stdout.println("[+] JsonParserTool");
        this.stdout.println("######################");
        callbacks.setExtensionName("JSON Extractor");

        // 创建自定义 UI
        SwingUtilities.invokeLater(() -> {
            mainPanel = new JPanel(new BorderLayout());

            // JSON 输入框
            jsonInputTextArea = new JTextArea(20, 30);
            mainPanel.add(new JScrollPane(jsonInputTextArea), BorderLayout.NORTH);

            // 输入参数框、执行按钮和路径列表
            JPanel inputPanel = new JPanel(new GridBagLayout());

            // 输入参数框
            fieldNameTextField = new JTextField(5); // 调整输入参数框的大小
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = 0;
            labelConstraints.weightx = 0.0;
            labelConstraints.anchor = GridBagConstraints.WEST;
            inputPanel.add(new JLabel("选择字段："), labelConstraints);

            GridBagConstraints textFieldConstraints = new GridBagConstraints();
            textFieldConstraints.gridx = 1;
            textFieldConstraints.gridy = 0;
            textFieldConstraints.weightx = 1.0;
            textFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
            inputPanel.add(fieldNameTextField, textFieldConstraints);


            // 执行按钮
            extractButton = new JButton("Extract");
            extractButton.setPreferredSize(new Dimension(80, 15)); // 调整按钮的大小
            GridBagConstraints buttonConstraints = new GridBagConstraints();
            buttonConstraints.gridx = 2;
            buttonConstraints.gridy = 0;
            buttonConstraints.weightx = 0.0;
            buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
            inputPanel.add(extractButton, buttonConstraints);

            // 路径列表
            DefaultListModel<String> pathListModel = new DefaultListModel<>();
            JList<String> pathList = new JList<>(pathListModel);
            pathList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            pathList.setVisibleRowCount(5); // 设置可见行数
            JScrollPane pathListScrollPane = new JScrollPane(pathList);
            GridBagConstraints pathListConstraints = new GridBagConstraints();
            pathListConstraints.gridx = 0;
            pathListConstraints.gridy = 1;
            pathListConstraints.gridwidth = 2;
            pathListConstraints.weightx = 1.0;
            pathListConstraints.weighty = 1.0;
            pathListConstraints.fill = GridBagConstraints.BOTH;
            inputPanel.add(pathListScrollPane, pathListConstraints);

            mainPanel.add(inputPanel, BorderLayout.CENTER);

            // 文本输出框
            outputTextArea = new JTextArea(20, 30);
            outputTextArea.setEditable(false);
            mainPanel.add(new JScrollPane(outputTextArea), BorderLayout.SOUTH);

            // 注册 UI 标签页
            callbacks.addSuiteTab(this);

            // 注册按钮点击事件监听器
            extractButton.addActionListener(e -> {
                String jsonText = jsonInputTextArea.getText();
                String fieldName = fieldNameTextField.getText();

                JsonObject jsonObject = JsonParser.parseString(jsonText).getAsJsonObject();
                List<String> paths = extractJsonPaths(jsonObject, fieldName);
                pathListModel.clear();
                paths.forEach(pathListModel::addElement);

                String outputText = extractJsonField(jsonText, fieldName);

                outputTextArea.setText(outputText);
            });

            ListSelectionListener listSelectionListener = e -> {
                if (!e.getValueIsAdjusting()) {
                    // 当用户完成选择时执行
                    int selectedIndex = pathList.getSelectedIndex();
                    if (selectedIndex != -1) {
                        String selectedPath = pathListModel.getElementAt(selectedIndex);
                        String jsonText = jsonInputTextArea.getText();
                        String fieldName = fieldNameTextField.getText();
                        JsonObject jsonObject = JsonParser.parseString(jsonText).getAsJsonObject();
                        String fieldValue = extractJsonFieldValue(jsonObject, selectedPath, fieldName);
                        outputTextArea.setText(fieldValue);
                    }
                }
            };

            // 将 ListSelectionListener 附加到路径列表
            pathList.addListSelectionListener(listSelectionListener);
        });



    }




    // 实现 ITab 接口的方法
    @Override
    public String getTabCaption() {
        return "JSON Extractor";
    }

    @Override
    public Component getUiComponent() {
        return mainPanel;
    }

    private static String extractJsonFieldValue(JsonObject jsonObject, String jsonPath, String fieldName) {
        List<String> fieldValues = new ArrayList<>();
        String[] keys = jsonPath.split("\\.");
        StringBuilder outputText = new StringBuilder();

        extractJsonFieldValueRecursive(jsonObject, keys, 0, fieldName, fieldValues);

        for (String value : fieldValues) {
            outputText.append(value).append("\n"); // 每个值之后添加换行符
        }
        return outputText.toString();
    }

    private static void extractJsonFieldValueRecursive(JsonElement element, String[] keys, int index, String fieldName, List<String> fieldValues) {
        if (element == null || index >= keys.length) {
            return;
        }

        String key = keys[index];

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(key)) {
                JsonElement nextElement = obj.get(key);
                if (index == keys.length - 1 && nextElement.isJsonPrimitive() && key.equals(fieldName)) {
                    fieldValues.add(nextElement.getAsString());
                } else {
                    extractJsonFieldValueRecursive(nextElement, keys, index + 1, fieldName, fieldValues);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            for (JsonElement arrayElement : jsonArray) {
                if (arrayElement.isJsonObject()) {
                    extractJsonFieldValueRecursive(arrayElement, keys, index, fieldName, fieldValues);
                }
            }
        }
    }


    // 编写提取 JSON 字段值的方法（类似之前示例）
    private static String extractJsonField(String jsonText, String fieldName) {
        List<String> fieldValues = new ArrayList<>();
        StringBuilder outputText = new StringBuilder();
        try {
        // 使用 Gson 解析 JSON
        JsonElement jsonElement = JsonParser.parseString(jsonText);
        findFieldValues(jsonElement, fieldName, fieldValues);

        for (String value : fieldValues) {
            outputText.append(value).append("\n"); // 每个值之后添加换行符
        }

        } catch (Exception e) {
            e.printStackTrace();
        outputText = new StringBuilder("解析失败：");
        }
        return outputText.toString();
    }


    private static List<String> extractJsonPaths(JsonObject jsonObject, String fieldName) {
        Set<String> jsonPaths = new HashSet<>();
        findFieldPaths(jsonObject, fieldName, "", jsonPaths);
        return new ArrayList<>(jsonPaths);
    }

    private static void findFieldPaths(JsonObject jsonObject, String fieldName, String currentPath, Set<String> jsonPaths) {
        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);
            String path = currentPath.isEmpty() ? key : currentPath + "." + key;

            if (key.equals(fieldName)) {
                jsonPaths.add(path);
            }

            if (element.isJsonObject()) {
                findFieldPaths(element.getAsJsonObject(), fieldName, path, jsonPaths);
            } else if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonElement arrayElement = jsonArray.get(i);
                    if (arrayElement.isJsonObject()) {
                        findFieldPaths(arrayElement.getAsJsonObject(), fieldName, path, jsonPaths);
                    }
                }
            }
        }
    }

    private static void findFieldValues(JsonElement jsonElement, String fieldName, List<String> fieldValues) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has(fieldName)) {
                JsonElement fieldElement = jsonObject.get(fieldName);
                if (fieldElement.isJsonPrimitive()) {
                    // 如果字段是基本数据类型，将其添加到结果列表
                    fieldValues.add(fieldElement.getAsString());
                } else if (fieldElement.isJsonObject() || fieldElement.isJsonArray()) {
                    // 如果字段是对象或数组，递归查找
                    findFieldValues(fieldElement, fieldName, fieldValues);
                }
            } else {
                // 如果字段不存在，遍历子元素
                for (String key : jsonObject.keySet()) {
                    JsonElement childElement = jsonObject.get(key);
                    findFieldValues(childElement, fieldName, fieldValues);
                }
            }
        } else if (jsonElement.isJsonArray()) {
            // 如果是数组，遍历数组元素
            for (JsonElement arrayElement : jsonElement.getAsJsonArray()) {
                findFieldValues(arrayElement, fieldName, fieldValues);
            }
        }
    }


}
