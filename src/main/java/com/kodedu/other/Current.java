package com.kodedu.other;

import com.kodedu.component.EditorPane;
import com.kodedu.component.MyTab;
import com.kodedu.controller.ApplicationController;
import com.kodedu.service.ThreadService;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by usta on 18.05.2014.
 */
@Component
public class Current {

    private final ApplicationController controller;
    private final ThreadService threadService;

    private Map<String, Integer> cache;
    private Path currentEpubPath;

    @Autowired
    public Current(final ApplicationController controller, final ThreadService threadService) {
        this.controller = controller;
        this.threadService = threadService;
    }

    public MyTab currentTab() {
        return (MyTab) controller.getTabPane().getSelectionModel().getSelectedItem();
    }

    public Optional<Path> currentPath() {
        return Optional.ofNullable(currentTab()).map(MyTab::getPath);
    }

    public WebView currentWebView() {
        return currentTab().getEditorPane().getWebView();
    }

    public EditorPane currentEditor() {
        MyTab currentTab = currentTab();
        if (Objects.isNull(currentTab)) {
            return null;
        }
        return currentTab.getEditorPane();
    }

    public WebEngine currentEngine() {
        return currentWebView().getEngine();
    }

    public Map<String, Integer> getCache() {
        if (Objects.isNull(cache))
            cache = new ConcurrentHashMap<String, Integer>();
        return cache;
    }

    public void setCache(Map<String, Integer> cache) {
        this.cache = cache;
    }

    public void setCurrentTabText(String currentTabText) {
        currentTab().setTabText(currentTabText);
    }

    public String getCurrentTabText() {
        return currentTab().getTabText();
    }

    public String currentEditorValue() {
        if (Platform.isFxApplicationThread())
            return (String) currentEngine().executeScript("editor.getValue()");

        final CompletableFuture<String> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            threadService.runActionLater(() -> {
                try {
                    Object result = currentEngine().executeScript("editor.getValue()");
                    completableFuture.complete((String) result);
                } catch (Exception ex) {
                    completableFuture.completeExceptionally(ex);
                }
            });
        }, threadService.executor());

        return completableFuture.join();

    }

    public String currentEditorSelection() {
        String value = (String) currentEngine().executeScript("editor.session.getTextRange(editor.getSelectionRange())");
        return value;
    }

    public void insertEditorValue(String content) {
        ((JSObject) currentEngine().executeScript("window")).setMember("insertValue", content);
        currentEngine().executeScript("editor.insert(insertValue)");
    }

    public void clearEditorValue() {
        currentEngine().executeScript(String.format("editor.setValue('')"));
    }

    public String currentEditorMode() {
        return currentEditor().editorMode();
    }

    public void setCurrentEpubPath(Path currentEpubPath) {
        this.currentEpubPath = currentEpubPath;
    }

    public Path getCurrentEpubPath() {
        return currentEpubPath;
    }

    public String currentClearTabText() {
        String tabText = getCurrentTabText().replace("*", "").trim();
        tabText = tabText.contains(".") ? tabText.split("\\.")[0] : tabText;
        return tabText;
    }
}
